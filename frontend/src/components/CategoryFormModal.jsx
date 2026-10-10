import { FolderPlus, Save } from 'lucide-react'
import { useState } from 'react'
import { apiRequest } from '../api/client'
import Modal from './Modal'

export default function CategoryFormModal({ category, title, description, onClose, onSaved }) {
  const editing = Boolean(category)
  const [form, setForm] = useState({ nombre: category?.nombre || '', descripcion: category?.descripcion || '' })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      const saved = await apiRequest(editing ? `/api/catalog/categories/${category.id}` : '/api/catalog/categories', {
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
    <Modal title={title || (editing ? 'Editar categoria' : 'Nueva categoria')} description={description || 'Organiza los productos para encontrarlos y reportarlos mejor.'} onClose={onClose} footer={<><button className="secondary-button" type="button" onClick={onClose}>Cancelar</button><button className="primary-button" type="submit" form="category-form" disabled={submitting}>{editing ? <Save size={18} /> : <FolderPlus size={18} />}{submitting ? 'Guardando...' : editing ? 'Guardar cambios' : 'Crear categoria'}</button></>}>
      <form id="category-form" className="form-grid" onSubmit={handleSubmit}>
        <div className="form-field form-field--full"><label htmlFor="category-form-name">Nombre *</label><input id="category-form-name" value={form.nombre} onChange={(event) => setForm((current) => ({ ...current, nombre: event.target.value }))} maxLength="100" required autoFocus /></div>
        <div className="form-field form-field--full"><label htmlFor="category-form-description">Descripcion</label><textarea id="category-form-description" rows="4" value={form.descripcion} onChange={(event) => setForm((current) => ({ ...current, descripcion: event.target.value }))} maxLength="255" /></div>
        {error && <p className="form-error form-field--full" role="alert">{error}</p>}
      </form>
    </Modal>
  )
}
