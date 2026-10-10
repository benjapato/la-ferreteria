import { Save, UserPlus } from 'lucide-react'
import { useState } from 'react'
import { apiRequest } from '../api/client'
import Modal from './Modal'

export default function UserFormModal({ user, onClose, onSaved }) {
  const editing = Boolean(user)
  const [form, setForm] = useState({
    nombreCompleto: user?.nombreCompleto || '',
    nombreUsuario: user?.nombreUsuario || '',
    contrasena: '',
    rol: user?.rol || 'VENDEDOR',
    activo: user?.activo ?? true,
  })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  function updateField(event) {
    const { name, value, checked, type } = event.target
    setForm((current) => ({ ...current, [name]: type === 'checkbox' ? checked : value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      const saved = await apiRequest(editing ? `/api/users/${user.id}` : '/api/users', {
        method: editing ? 'PUT' : 'POST',
        body: form,
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
      title={editing ? 'Editar usuario' : 'Nuevo usuario'}
      description={editing ? 'Actualiza sus datos, rol o acceso al sistema.' : 'Crea un acceso personal para trabajar en la caja.'}
      onClose={onClose}
      footer={<><button className="secondary-button" type="button" onClick={onClose} disabled={submitting}>Cancelar</button><button className="primary-button" type="submit" form="user-form" disabled={submitting}>{editing ? <Save size={18} /> : <UserPlus size={18} />}{submitting ? 'Guardando...' : editing ? 'Guardar cambios' : 'Crear usuario'}</button></>}
    >
      <form id="user-form" className="form-grid form-grid--two" onSubmit={handleSubmit}>
        <div className="form-field form-field--full"><label htmlFor="user-full-name">Nombre completo *</label><input id="user-full-name" name="nombreCompleto" value={form.nombreCompleto} onChange={updateField} maxLength="120" required autoFocus /></div>
        <div className="form-field"><label htmlFor="user-name">Nombre de usuario *</label><input id="user-name" name="nombreUsuario" value={form.nombreUsuario} onChange={updateField} minLength="3" maxLength="60" required autoComplete="off" /></div>
        <div className="form-field"><label htmlFor="user-role">Rol *</label><select id="user-role" name="rol" value={form.rol} onChange={updateField}><option value="VENDEDOR">Vendedor</option><option value="ADMINISTRADOR">Administrador</option></select></div>
        <div className="form-field form-field--full"><label htmlFor="user-password">{editing ? 'Nueva contrasena' : 'Contrasena *'}</label><input id="user-password" name="contrasena" type="password" value={form.contrasena} onChange={updateField} minLength={editing ? undefined : 8} maxLength="100" required={!editing} autoComplete="new-password" placeholder={editing ? 'Dejar vacio para conservar la actual' : 'Minimo 8 caracteres'} /><small>{editing ? 'Solo se modifica si ingresas una nueva.' : 'Usa una clave que no comparta con otros usuarios.'}</small></div>
        <label className="switch-field form-field--full"><input name="activo" type="checkbox" checked={form.activo} onChange={updateField} /><span aria-hidden="true" /><div><strong>Usuario activo</strong><small>Puede iniciar sesion y operar en el sistema.</small></div></label>
        {error && <p className="form-error form-field--full" role="alert">{error}</p>}
      </form>
    </Modal>
  )
}
