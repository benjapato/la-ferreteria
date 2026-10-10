import { ArrowDownRight, ArrowUpRight, Percent } from 'lucide-react'
import { useMemo, useState } from 'react'
import { apiRequest } from '../api/client'
import Modal from './Modal'

const moneyFormatter = new Intl.NumberFormat('es-AR', {
  style: 'currency',
  currency: 'ARS',
})

export default function BulkPriceUpdateModal({ totalProducts, onClose, onUpdated }) {
  const [operation, setOperation] = useState('AUMENTAR')
  const [percentage, setPercentage] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const parsedPercentage = useMemo(() => {
    const value = Number(percentage.trim().replace(',', '.'))
    return Number.isFinite(value) ? value : 0
  }, [percentage])
  const validPercentage = parsedPercentage > 0
    && (operation === 'AUMENTAR' || parsedPercentage < 100)
  const examplePrice = validPercentage
    ? 10000 * (operation === 'AUMENTAR'
      ? 1 + parsedPercentage / 100
      : 1 - parsedPercentage / 100)
    : 10000

  async function handleSubmit(event) {
    event.preventDefault()
    if (!validPercentage || totalProducts === 0) return

    setSubmitting(true)
    setError('')
    try {
      const result = await apiRequest('/api/catalog/products/prices/percentage', {
        method: 'POST',
        body: { porcentaje: parsedPercentage, operacion: operation },
      })
      onUpdated(result)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      title="Actualizar precios"
      description="Modifica el precio de venta de todos los productos con un solo porcentaje."
      onClose={onClose}
      footer={<><button className="secondary-button" type="button" onClick={onClose}>Cancelar</button><button className="primary-button" type="submit" form="bulk-price-form" disabled={submitting || !validPercentage || totalProducts === 0}><Percent size={18} />{submitting ? 'Actualizando...' : `Actualizar ${totalProducts} productos`}</button></>}
    >
      <form id="bulk-price-form" className="bulk-price-form" onSubmit={handleSubmit}>
        <div className="price-operation" role="group" aria-label="Tipo de actualizacion">
          <button className={operation === 'AUMENTAR' ? 'price-operation__option price-operation__option--active' : 'price-operation__option'} type="button" aria-pressed={operation === 'AUMENTAR'} onClick={() => { setOperation('AUMENTAR'); setError('') }}><ArrowUpRight size={20} /><span><strong>Aumentar</strong><small>Subir precios de venta</small></span></button>
          <button className={operation === 'DISMINUIR' ? 'price-operation__option price-operation__option--active price-operation__option--decrease' : 'price-operation__option'} type="button" aria-pressed={operation === 'DISMINUIR'} onClick={() => { setOperation('DISMINUIR'); setError('') }}><ArrowDownRight size={20} /><span><strong>Disminuir</strong><small>Bajar precios de venta</small></span></button>
        </div>

        <div className="form-field">
          <label htmlFor="bulk-price-percentage">Porcentaje *</label>
          <div className="bulk-price-input"><input id="bulk-price-percentage" value={percentage} onChange={(event) => setPercentage(event.target.value)} inputMode="decimal" placeholder="Ejemplo: 10" autoFocus /><span>%</span></div>
          {operation === 'DISMINUIR' && parsedPercentage >= 100 && <span className="field-error">La disminucion debe ser menor al 100%.</span>}
        </div>

        <div className="bulk-price-preview">
          <div><span>Ejemplo</span><strong>{moneyFormatter.format(10000)} {operation === 'AUMENTAR' ? <ArrowUpRight size={15} /> : <ArrowDownRight size={15} />} {moneyFormatter.format(examplePrice)}</strong></div>
          <div><span>Productos alcanzados</span><strong>{totalProducts}</strong></div>
        </div>

        <p className="bulk-price-note">Se incluyen productos activos e inactivos. Al aumentar, los precios antiguos que esten bajo el costo se llevaran como minimo al costo. Al disminuir, si uno queda bajo el costo no se modificara ningun producto.</p>
        {error && <p className="form-error" role="alert">{error}</p>}
      </form>
    </Modal>
  )
}
