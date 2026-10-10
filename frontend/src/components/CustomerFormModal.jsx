import { Save, UserPlus } from 'lucide-react'
import { useState } from 'react'
import { apiRequest } from '../api/client'
import Modal from './Modal'

const emptyForm = {
  nombre: '', documento: '', telefono: '', email: '', direccion: '',
}

export default function CustomerFormModal({ customer, onClose, onSaved }) {
  const editing = Boolean(customer)
  const [form, setForm] = useState(() => customer ? {
    nombre: customer.nombre || '',
    documento: customer.documento || '',
    telefono: customer.telefono || '',
    email: customer.email || '',
    direccion: customer.direccion || '',
  } : emptyForm)
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
      const saved = await apiRequest(
        editing ? `/api/customers/${customer.id}` : '/api/customers',
        { method: editing ? 'PUT' : 'POST', body: form },
      )
      onSaved(saved)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      title={editing ? 'Editar cliente' : 'Nuevo cliente'}
      description={editing ? 'Actualiza sus datos de contacto. El saldo no se modifica desde aqui.' : 'Registra sus datos para ventas y cuenta corriente.'}
      onClose={onClose}
      footer={(
        <>
          <button className="secondary-button" type="button" onClick={onClose} disabled={submitting}>Cancelar</button>
          <button className="primary-button" type="submit" form="customer-form" disabled={submitting}>
            {editing ? <Save size={18} /> : <UserPlus size={18} />}
            {submitting ? 'Guardando...' : editing ? 'Guardar cambios' : 'Crear cliente'}
          </button>
        </>
      )}
    >
      <form id="customer-form" className="form-grid" onSubmit={handleSubmit}>
        <div className="form-field form-field--span-two">
          <label htmlFor="customer-name">Nombre *</label>
          <input id="customer-name" name="nombre" maxLength="150" required autoFocus value={form.nombre} onChange={updateField} />
        </div>
        <div className="form-field">
          <label htmlFor="customer-document">Documento / CUIT</label>
          <input id="customer-document" name="documento" maxLength="30" value={form.documento} onChange={updateField} />
        </div>
        <div className="form-field">
          <label htmlFor="customer-phone">Telefono</label>
          <input id="customer-phone" name="telefono" type="tel" maxLength="50" value={form.telefono} onChange={updateField} />
        </div>
        <div className="form-field form-field--span-two">
          <label htmlFor="customer-email">Correo electronico</label>
          <input id="customer-email" name="email" type="email" maxLength="150" value={form.email} onChange={updateField} />
        </div>
        <div className="form-field form-field--full">
          <label htmlFor="customer-address">Direccion</label>
          <input id="customer-address" name="direccion" maxLength="250" value={form.direccion} onChange={updateField} />
        </div>
      </form>
      {error && <p className="form-error" role="alert">{error}</p>}
    </Modal>
  )
}
