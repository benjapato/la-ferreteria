import { useState } from 'react'
import { UserPlus } from 'lucide-react'
import { apiRequest } from '../api/client'
import Modal from './Modal'

const initialForm = { nombre: '', documento: '', telefono: '', email: '', direccion: '' }

export default function QuickClientModal({ onClose, onCreated }) {
  const [form, setForm] = useState(initialForm)
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
      const client = await apiRequest('/api/sales/clients', { method: 'POST', body: form })
      onCreated(client)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      title="Nuevo cliente"
      description="Se seleccionara automaticamente para esta venta."
      onClose={onClose}
      footer={(
        <>
          <button className="secondary-button" type="button" onClick={onClose}>Cancelar</button>
          <button className="primary-button" type="submit" form="quick-client-form" disabled={submitting}>
            <UserPlus size={18} />{submitting ? 'Guardando...' : 'Crear cliente'}
          </button>
        </>
      )}
    >
      <form id="quick-client-form" className="form-grid" onSubmit={handleSubmit}>
        <div className="form-field form-field--full"><label htmlFor="client-name">Nombre *</label><input id="client-name" name="nombre" value={form.nombre} onChange={updateField} maxLength="150" required autoFocus /></div>
        <div className="form-field"><label htmlFor="client-document">DNI, CUIT o CUIL</label><input id="client-document" name="documento" value={form.documento} onChange={updateField} maxLength="30" /></div>
        <div className="form-field"><label htmlFor="client-phone">Telefono</label><input id="client-phone" name="telefono" value={form.telefono} onChange={updateField} maxLength="50" /></div>
        <div className="form-field form-field--full"><label htmlFor="client-email">Correo electronico</label><input id="client-email" name="email" type="email" value={form.email} onChange={updateField} maxLength="150" /></div>
        <div className="form-field form-field--full"><label htmlFor="client-address">Direccion</label><input id="client-address" name="direccion" value={form.direccion} onChange={updateField} maxLength="250" /></div>
        {error && <p className="form-error form-field--full" role="alert">{error}</p>}
      </form>
    </Modal>
  )
}
