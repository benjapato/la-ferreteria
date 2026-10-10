import { ArrowDown, ArrowUp, RefreshCw } from 'lucide-react'
import { useState } from 'react'
import { apiRequest } from '../api/client'
import Modal from './Modal'

export default function StockAdjustmentModal({ product, onClose, onSaved }) {
  const [form, setForm] = useState({ tipo: 'AJUSTE_ENTRADA', cantidad: '', observacion: '' })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const quantity = Number(form.cantidad || 0)
  const currentStock = Number(product.stockActual)
  const resultingStock = form.tipo === 'AJUSTE_ENTRADA' ? currentStock + quantity : currentStock - quantity

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      const saved = await apiRequest(`/api/catalog/products/${product.id}/stock-adjustments`, {
        method: 'POST',
        body: { ...form, cantidad: quantity },
      })
      onSaved(saved)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      title={`Ajustar stock de ${product.nombre}`}
      description={`Stock registrado: ${formatStock(product.stockActual)} ${formatLabel(product.unidadMedida)}`}
      onClose={onClose}
      footer={(
        <>
          <button className="secondary-button" type="button" onClick={onClose} disabled={submitting}>Cancelar</button>
          <button className="primary-button" type="submit" form="stock-adjustment-form" disabled={submitting || quantity <= 0 || resultingStock < 0}><RefreshCw size={18} />{submitting ? 'Registrando...' : 'Registrar ajuste'}</button>
        </>
      )}
    >
      <form id="stock-adjustment-form" className="stock-adjustment-form" onSubmit={handleSubmit}>
        <div className="adjustment-type" role="group" aria-label="Tipo de ajuste">
          <button className={form.tipo === 'AJUSTE_ENTRADA' ? 'adjustment-option adjustment-option--active' : 'adjustment-option'} type="button" onClick={() => setForm((current) => ({ ...current, tipo: 'AJUSTE_ENTRADA' }))}><ArrowUp size={19} /><span><strong>Entrada</strong><small>Agregar unidades</small></span></button>
          <button className={form.tipo === 'AJUSTE_SALIDA' ? 'adjustment-option adjustment-option--active adjustment-option--out' : 'adjustment-option'} type="button" onClick={() => setForm((current) => ({ ...current, tipo: 'AJUSTE_SALIDA' }))}><ArrowDown size={19} /><span><strong>Salida</strong><small>Descontar unidades</small></span></button>
        </div>
        <div className="form-field"><label htmlFor="adjustment-quantity">Cantidad *</label><input id="adjustment-quantity" type="number" min="0.001" step="0.001" value={form.cantidad} onChange={(event) => setForm((current) => ({ ...current, cantidad: event.target.value }))} required autoFocus /></div>
        <div className={resultingStock < 0 ? 'stock-result stock-result--invalid' : 'stock-result'}><span>Stock resultante</span><strong>{formatStock(resultingStock)}</strong></div>
        <div className="form-field"><label htmlFor="adjustment-note">Observacion</label><textarea id="adjustment-note" rows="3" maxLength="500" value={form.observacion} onChange={(event) => setForm((current) => ({ ...current, observacion: event.target.value }))} placeholder="Opcional" /></div>
        {error && <p className="form-error" role="alert">{error}</p>}
      </form>
    </Modal>
  )
}

function formatStock(value) {
  return new Intl.NumberFormat('es-AR', { maximumFractionDigits: 3 }).format(Number(value || 0))
}

function formatLabel(value) {
  return String(value || '').toLowerCase().replaceAll('_', ' ')
}
