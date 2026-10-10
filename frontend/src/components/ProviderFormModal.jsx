import { Save, Truck } from 'lucide-react'
import { useState } from 'react'
import { apiRequest } from '../api/client'
import Modal from './Modal'

const emptyProvider = { nombre: '', razonSocial: '', cuit: '', telefono: '', email: '', direccion: '' }

export default function ProviderFormModal({ provider, onClose, onSaved }) {
  const editing = Boolean(provider)
  const [form, setForm] = useState(provider ? {
    nombre: provider.nombre,
    razonSocial: provider.razonSocial || '',
    cuit: provider.cuit || '',
    telefono: provider.telefono || '',
    email: provider.email || '',
    direccion: provider.direccion || '',
  } : emptyProvider)
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
      const saved = await apiRequest(editing ? `/api/catalog/providers/${provider.id}` : '/api/catalog/providers', {
        method: editing ? 'PUT' : 'POST', body: form,
      })
      onSaved(saved)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal title={editing ? 'Editar proveedor' : 'Nuevo proveedor'} description="Datos de contacto para compras y reposicion." onClose={onClose} size="large" footer={<><button className="secondary-button" type="button" onClick={onClose}>Cancelar</button><button className="primary-button" type="submit" form="provider-form" disabled={submitting}>{editing ? <Save size={18} /> : <Truck size={18} />}{submitting ? 'Guardando...' : editing ? 'Guardar cambios' : 'Crear proveedor'}</button></>}>
      <form id="provider-form" className="form-grid form-grid--three" onSubmit={handleSubmit}>
        <div className="form-field"><label htmlFor="provider-name">Nombre *</label><input id="provider-name" name="nombre" value={form.nombre} onChange={updateField} maxLength="150" required autoFocus /></div>
        <div className="form-field form-field--span-two"><label htmlFor="provider-business-name">Razon social</label><input id="provider-business-name" name="razonSocial" value={form.razonSocial} onChange={updateField} maxLength="150" /></div>
        <div className="form-field"><label htmlFor="provider-cuit">CUIT</label><input id="provider-cuit" name="cuit" value={form.cuit} onChange={updateField} maxLength="13" placeholder="30-12345678-9" /></div>
        <div className="form-field"><label htmlFor="provider-phone">Telefono</label><input id="provider-phone" name="telefono" value={form.telefono} onChange={updateField} maxLength="30" /></div>
        <div className="form-field"><label htmlFor="provider-email">Correo</label><input id="provider-email" name="email" type="email" value={form.email} onChange={updateField} maxLength="150" /></div>
        <div className="form-field form-field--full"><label htmlFor="provider-address">Direccion</label><input id="provider-address" name="direccion" value={form.direccion} onChange={updateField} maxLength="255" /></div>
        {error && <p className="form-error form-field--full" role="alert">{error}</p>}
      </form>
    </Modal>
  )
}
