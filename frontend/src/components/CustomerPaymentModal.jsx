import { AlertTriangle, Banknote, HandCoins } from 'lucide-react'
import { useState } from 'react'
import { apiRequest } from '../api/client'
import Modal from './Modal'

const defaultPaymentMethods = ['EFECTIVO', 'TRANSFERENCIA', 'TARJETA_DEBITO', 'TARJETA_CREDITO', 'OTRO']
const moneyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })

export default function CustomerPaymentModal({ customer, cashOpen, paymentMethods = defaultPaymentMethods, onClose, onSaved }) {
  const debt = Math.max(Number(customer.saldo || 0), 0)
  const [form, setForm] = useState({ monto: debt > 0 ? debt.toFixed(2) : '', medioPago: paymentMethods[0] || 'EFECTIVO', observacion: '' })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      const saved = await apiRequest(`/api/customers/${customer.id}/payments`, {
        method: 'POST',
        body: { ...form, monto: Number(form.monto) },
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
      title={`Registrar cobro a ${customer.nombre}`}
      description="El importe se descuenta del saldo del cliente y se registra en la caja abierta."
      onClose={onClose}
      footer={(
        <>
          <button className="secondary-button" type="button" onClick={onClose} disabled={submitting}>Cancelar</button>
          <button className="primary-button" type="submit" form="customer-payment-form" disabled={submitting || !cashOpen || !customer.activo}>
            <HandCoins size={18} />{submitting ? 'Registrando...' : 'Registrar cobro'}
          </button>
        </>
      )}
    >
      {!cashOpen && <div className="cash-warning customer-payment-warning"><AlertTriangle size={20} /><div><strong>La caja esta cerrada</strong><span>Abre la caja desde Inicio antes de registrar el cobro.</span></div></div>}
      {!customer.activo && <div className="cash-warning customer-payment-warning"><AlertTriangle size={20} /><div><strong>El cliente esta inactivo</strong><span>Activalo antes de registrar un nuevo cobro.</span></div></div>}
      <div className="customer-payment-balance"><span>Saldo actual</span><strong className={`balance-value balance-value--${String(customer.estadoSaldo).toLowerCase()}`}>{moneyFormatter.format(Number(customer.saldo || 0))}</strong><small>{customer.estadoSaldo === 'DEUDA' ? 'Deuda pendiente' : customer.estadoSaldo === 'A_FAVOR' ? 'Saldo a favor' : 'Cuenta al dia'}</small></div>
      <form id="customer-payment-form" className="form-grid customer-payment-form" onSubmit={handleSubmit}>
        <div className="form-field">
          <label htmlFor="customer-payment-amount">Importe *</label>
          <div className="amount-input"><span>$</span><input id="customer-payment-amount" name="monto" type="number" min="0.01" step="0.01" required autoFocus value={form.monto} onChange={updateField} /></div>
          {debt > 0 && <button className="text-action" type="button" onClick={() => setForm((current) => ({ ...current, monto: debt.toFixed(2) }))}><Banknote size={14} />Saldar deuda</button>}
        </div>
        <div className="form-field">
          <label htmlFor="customer-payment-method">Medio de pago *</label>
          <select id="customer-payment-method" name="medioPago" value={form.medioPago} onChange={updateField}>{paymentMethods.map((method) => <option key={method} value={method}>{formatLabel(method)}</option>)}</select>
        </div>
        <div className="form-field form-field--full">
          <label htmlFor="customer-payment-note">Observacion</label>
          <textarea id="customer-payment-note" name="observacion" maxLength="300" rows="3" placeholder="Opcional" value={form.observacion} onChange={updateField} />
          <span className="field-note">Si el pago supera la deuda, la diferencia queda a favor del cliente.</span>
        </div>
      </form>
      {error && <p className="form-error" role="alert">{error}</p>}
    </Modal>
  )
}

function formatLabel(value) {
  return String(value || '').toLowerCase().replaceAll('_', ' ').replace(/^./, (letter) => letter.toUpperCase())
}
