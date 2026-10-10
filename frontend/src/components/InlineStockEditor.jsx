import { Check, PencilLine, X } from 'lucide-react'
import { useState } from 'react'
import { apiRequest } from '../api/client'

const stockFormatter = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 3 })

export default function InlineStockEditor({ product, editable, onSaved }) {
  const [editing, setEditing] = useState(false)
  const [value, setValue] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  function startEditing() {
    setValue(String(product.stockActual ?? 0).replace('.', ','))
    setError('')
    setEditing(true)
  }

  function cancelEditing() {
    if (submitting) return
    setEditing(false)
    setError('')
  }

  async function saveStock(event) {
    event.preventDefault()
    const nextStock = parseStock(value)
    if (nextStock == null) {
      setError('Escribi un stock valido de hasta 3 decimales.')
      return
    }

    const currentStock = Math.round(Number(product.stockActual || 0) * 1000)
    if (nextStock === currentStock) {
      cancelEditing()
      return
    }

    setSubmitting(true)
    setError('')
    try {
      const saved = await apiRequest(`/api/catalog/products/${product.id}/stock-adjustments`, {
        method: 'POST',
        body: {
          tipo: nextStock > currentStock ? 'AJUSTE_ENTRADA' : 'AJUSTE_SALIDA',
          cantidad: Math.abs(nextStock - currentStock) / 1000,
          observacion: 'Stock actualizado desde el catalogo',
        },
      })
      setEditing(false)
      await onSaved(saved)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setSubmitting(false)
    }
  }

  function handleKeyDown(event) {
    if (event.key === 'Escape') {
      event.preventDefault()
      cancelEditing()
    }
  }

  if (!editing) {
    const content = <StockContent product={product} editable={editable} />
    return editable ? (
      <button className="stock-cell stock-cell--editable" type="button" title="Cambiar stock" aria-label={`Cambiar stock de ${product.nombre}`} onClick={startEditing}>
        {content}
      </button>
    ) : <div className="stock-cell">{content}</div>
  }

  return (
    <form className="inline-stock-editor" onSubmit={saveStock}>
      <input
        type="text"
        inputMode="decimal"
        aria-label={`Nuevo stock de ${product.nombre}`}
        value={value}
        onChange={(event) => setValue(event.target.value)}
        onKeyDown={handleKeyDown}
        disabled={submitting}
        autoFocus
      />
      <button className="icon-button inline-stock-editor__save" type="submit" title="Guardar stock" aria-label={`Guardar stock de ${product.nombre}`} disabled={submitting}><Check size={16} /></button>
      <button className="icon-button" type="button" title="Cancelar" aria-label={`Cancelar cambio de stock de ${product.nombre}`} onClick={cancelEditing} disabled={submitting}><X size={16} /></button>
      {error && <small className="inline-stock-editor__error" role="alert">{error}</small>}
    </form>
  )
}

function StockContent({ product, editable }) {
  return (
    <>
      <strong>{stockFormatter.format(Number(product.stockActual))}</strong>
      <span>{formatLabel(product.unidadMedida)}</span>
      {editable && <PencilLine className="stock-cell__edit" size={12} aria-hidden="true" />}
      <small className={`stock-state stock-state--${product.estadoStock.toLowerCase()}`}>{product.estadoStock === 'NORMAL' ? 'Normal' : product.estadoStock === 'BAJO' ? 'Bajo' : 'Sin stock'}</small>
    </>
  )
}

function parseStock(value) {
  const normalized = value.trim().replace(',', '.')
  if (!/^\d{1,12}(?:\.\d{1,3})?$/.test(normalized)) return null
  const [integer, decimal = ''] = normalized.split('.')
  const stock = Number(integer) * 1000 + Number(decimal.padEnd(3, '0'))
  return Number.isSafeInteger(stock) ? stock : null
}

function formatLabel(value) {
  return String(value || '').toLowerCase().replaceAll('_', ' ')
}
