import { ArrowDownToLine, ArrowUpFromLine, Save } from 'lucide-react'
import { useState } from 'react'
import { apiRequest } from '../api/client'
import Modal from './Modal'

const moneyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', minimumFractionDigits: 2 })

export default function CashMovementModal({ initialType = 'INGRESO', expectedCash, onClose, onSaved }) {
  const [form, setForm] = useState({ tipo: initialType, monto: '', motivo: '' })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  async function submit(event) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      const cash = await apiRequest('/api/cash/movements', {
        method: 'POST',
        body: { ...form, monto: Number(form.monto), motivo: form.motivo.trim() || null },
      })
      onSaved(cash)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setSubmitting(false)
    }
  }

  const withdrawal = form.tipo === 'RETIRO'

  return (
    <Modal title="Movimiento de efectivo" description={`Efectivo esperado: ${moneyFormatter.format(Number(expectedCash || 0))}`} onClose={onClose} footer={<><button className="secondary-button" type="button" onClick={onClose} disabled={submitting}>Cancelar</button><button className="primary-button" type="submit" form="cash-movement-form" disabled={submitting}><Save size={18} />{submitting ? 'Registrando...' : 'Registrar'}</button></>}>
      <form id="cash-movement-form" className="cash-movement-form" onSubmit={submit}>
        <div className="cash-movement-types" role="group" aria-label="Tipo de movimiento">
          <button className={form.tipo === 'INGRESO' ? 'cash-movement-type cash-movement-type--active' : 'cash-movement-type'} type="button" onClick={() => setForm((current) => ({ ...current, tipo: 'INGRESO' }))}><ArrowDownToLine size={20} /><span><strong>Ingreso</strong><small>Agregar efectivo</small></span></button>
          <button className={withdrawal ? 'cash-movement-type cash-movement-type--active cash-movement-type--withdrawal' : 'cash-movement-type'} type="button" onClick={() => setForm((current) => ({ ...current, tipo: 'RETIRO' }))}><ArrowUpFromLine size={20} /><span><strong>Retiro</strong><small>Sacar efectivo</small></span></button>
        </div>
        <div className="form-field"><label htmlFor="cash-movement-amount">Monto *</label><input id="cash-movement-amount" name="monto" type="number" min="0.01" step="0.01" value={form.monto} onChange={updateField} required autoFocus /></div>
        <div className="form-field"><label htmlFor="cash-movement-reason">Motivo (opcional)</label><input id="cash-movement-reason" name="motivo" value={form.motivo} onChange={updateField} maxLength="200" placeholder={withdrawal ? 'Ej. Pago de flete' : 'Ej. Cambio adicional'} /><small>Si queda vacio, se guardara como Sin motivo.</small></div>
        {withdrawal && Number(form.monto) > Number(expectedCash) && <p className="form-error">El retiro supera el efectivo esperado en caja.</p>}
        {error && <p className="form-error" role="alert">{error}</p>}
      </form>
    </Modal>
  )
}
