import { PackagePlus, Save } from 'lucide-react'
import { useState } from 'react'
import { apiRequest } from '../api/client'
import Modal from './Modal'

const emptyForm = {
  codigo: '', nombre: '', marca: '', descripcion: '', precioCompra: '', precioVenta: '',
  stockActual: '0', stockMinimo: '0', unidadMedida: 'UNIDAD', categoriaId: '',
}

export default function ProductFormModal({ product, categories, units, onClose, onSaved }) {
  const editing = Boolean(product)
  const [form, setForm] = useState(() => product ? {
    codigo: product.codigo,
    nombre: product.nombre,
    marca: product.marca || '',
    descripcion: product.descripcion || '',
    precioCompra: String(product.precioCompra),
    precioVenta: String(product.precioVenta),
    stockActual: String(product.stockActual),
    stockMinimo: String(product.stockMinimo),
    unidadMedida: product.unidadMedida,
    categoriaId: String(product.categoriaId),
  } : { ...emptyForm, categoriaId: String(categories.find((category) => category.activa)?.id || '') })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const purchasePrice = Number(form.precioCompra || 0)
  const salePrice = Number(form.precioVenta || 0)
  const margin = salePrice - purchasePrice
  const priceInvalid = form.precioCompra !== '' && form.precioVenta !== '' && salePrice < purchasePrice

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (priceInvalid) {
      setError('El precio de venta no puede ser menor al precio de compra.')
      return
    }
    setSubmitting(true)
    setError('')
    try {
      const saved = await apiRequest(
        editing ? `/api/catalog/products/${product.id}` : '/api/catalog/products',
        {
          method: editing ? 'PUT' : 'POST',
          body: {
            ...form,
            categoriaId: Number(form.categoriaId),
            precioCompra: Number(form.precioCompra),
            precioVenta: Number(form.precioVenta),
            stockActual: Number(form.stockActual),
            stockMinimo: Number(form.stockMinimo),
          },
        },
      )
      onSaved(saved)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setSubmitting(false)
    }
  }

  const selectableCategories = categories.filter((category) => category.activa || category.id === product?.categoriaId)

  return (
    <Modal
      title={editing ? `Editar ${product.nombre}` : 'Nuevo producto'}
      description={editing ? 'Los cambios de stock se registran desde la accion de ajuste.' : 'Completa los datos comerciales y el stock inicial.'}
      onClose={onClose}
      size="large"
      footer={(
        <>
          <button className="secondary-button" type="button" onClick={onClose} disabled={submitting}>Cancelar</button>
          <button className="primary-button" type="submit" form="product-form" disabled={submitting || !form.categoriaId || priceInvalid}>
            {editing ? <Save size={18} /> : <PackagePlus size={18} />}{submitting ? 'Guardando...' : editing ? 'Guardar cambios' : 'Crear producto'}
          </button>
        </>
      )}
    >
      <form id="product-form" className="form-grid form-grid--three direct-number-entry" onSubmit={handleSubmit}>
        <div className="form-field"><label htmlFor="catalog-product-code">Codigo *</label><input id="catalog-product-code" name="codigo" value={form.codigo} onChange={updateField} maxLength="50" required autoFocus /></div>
        <div className="form-field form-field--span-two"><label htmlFor="catalog-product-name">Nombre *</label><input id="catalog-product-name" name="nombre" value={form.nombre} onChange={updateField} maxLength="150" required /></div>
        <div className="form-field"><label htmlFor="catalog-product-brand">Marca</label><input id="catalog-product-brand" name="marca" value={form.marca} onChange={updateField} maxLength="100" /></div>
        <div className="form-field form-field--span-two"><label htmlFor="catalog-product-description">Descripcion</label><input id="catalog-product-description" name="descripcion" value={form.descripcion} onChange={updateField} maxLength="500" /></div>
        <div className="form-field"><label htmlFor="catalog-purchase-price">Precio de compra *</label><div className="money-input"><span>$</span><input id="catalog-purchase-price" name="precioCompra" type="number" min="0" step="0.01" value={form.precioCompra} onChange={updateField} required /></div></div>
        <div className="form-field"><label htmlFor="catalog-sale-price">Precio de venta *</label><div className={priceInvalid ? 'money-input money-input--invalid' : 'money-input'}><span>$</span><input id="catalog-sale-price" name="precioVenta" type="number" min={Math.max(0.01, purchasePrice)} step="0.01" value={form.precioVenta} onChange={updateField} aria-invalid={priceInvalid} required /></div>{priceInvalid && <small className="field-error">Debe ser igual o mayor al precio de compra.</small>}</div>
        <div className={margin < 0 ? 'margin-preview margin-preview--negative' : 'margin-preview'}><span>Margen unitario</span><strong>{formatMoney(margin)}</strong></div>
        <div className="form-field"><label htmlFor="catalog-current-stock">{editing ? 'Stock actual' : 'Stock inicial *'}</label><input id="catalog-current-stock" name="stockActual" type="number" min="0" step="0.001" value={form.stockActual} onChange={updateField} readOnly={editing} required />{editing && <small className="field-note">Usa Ajustar stock para modificarlo.</small>}</div>
        <div className="form-field"><label htmlFor="catalog-minimum-stock">Stock minimo *</label><input id="catalog-minimum-stock" name="stockMinimo" type="number" min="0" step="0.001" value={form.stockMinimo} onChange={updateField} required /></div>
        <div className="form-field"><label htmlFor="catalog-product-unit">Unidad *</label><select id="catalog-product-unit" name="unidadMedida" value={form.unidadMedida} onChange={updateField}>{units.map((unit) => <option key={unit} value={unit}>{formatLabel(unit)}</option>)}</select></div>
        <div className="form-field form-field--span-two"><label htmlFor="catalog-product-category">Categoria *</label><select id="catalog-product-category" name="categoriaId" value={form.categoriaId} onChange={updateField} required><option value="" disabled>Seleccionar categoria</option>{selectableCategories.map((category) => <option key={category.id} value={category.id}>{category.nombre}{category.activa ? '' : ' (inactiva)'}</option>)}</select></div>
        {error && <p className="form-error form-field--full" role="alert">{error}</p>}
      </form>
    </Modal>
  )
}

function formatMoney(value) {
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(Number(value || 0))
}

function formatLabel(value) {
  return String(value || '').toLowerCase().replaceAll('_', ' ').replace(/^./, (letter) => letter.toUpperCase())
}
