import { useState } from 'react'
import { FolderPlus, PackagePlus, Plus, X } from 'lucide-react'
import { apiRequest } from '../api/client'
import Modal from './Modal'

const initialForm = {
  codigo: '', nombre: '', marca: '', descripcion: '', precioCompra: '', precioVenta: '',
  stockActual: '', stockMinimo: '', unidadMedida: 'UNIDAD', categoriaId: '',
}

export default function QuickProductModal({ categories, units, onClose, onCreated, onCategoryCreated }) {
  const [form, setForm] = useState(() => ({ ...initialForm, categoriaId: categories[0]?.id || '' }))
  const [availableCategories, setAvailableCategories] = useState(categories)
  const [showCategoryForm, setShowCategoryForm] = useState(categories.length === 0)
  const [categoryName, setCategoryName] = useState('')
  const [categorySubmitting, setCategorySubmitting] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const purchasePrice = Number(form.precioCompra || 0)
  const salePrice = Number(form.precioVenta || 0)
  const priceInvalid = form.precioCompra !== '' && form.precioVenta !== '' && salePrice < purchasePrice
  const stockInvalid = form.stockActual !== '' && Number(form.stockActual) <= 0
  const formInvalid = priceInvalid || stockInvalid || !form.categoriaId

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  async function createCategory() {
    if (!categoryName.trim()) return
    setCategorySubmitting(true)
    setError('')
    try {
      const category = await apiRequest('/api/sales/categories', {
        method: 'POST', body: { nombre: categoryName.trim(), descripcion: '' },
      })
      setAvailableCategories((current) => [...current, category])
      setForm((current) => ({ ...current, categoriaId: category.id }))
      setCategoryName('')
      setShowCategoryForm(false)
      onCategoryCreated(category)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setCategorySubmitting(false)
    }
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (stockInvalid) {
      setError('El stock actual debe ser mayor que cero.')
      return
    }
    if (priceInvalid) {
      setError('El precio de venta no puede ser menor al precio de compra.')
      return
    }
    setSubmitting(true)
    setError('')
    try {
      const product = await apiRequest('/api/sales/products', {
        method: 'POST',
        body: {
          ...form,
          categoriaId: Number(form.categoriaId),
          precioCompra: Number(form.precioCompra),
          precioVenta: Number(form.precioVenta),
          stockActual: Number(form.stockActual),
          stockMinimo: Number(form.stockMinimo),
        },
      })
      onCreated(product)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      title="Nuevo producto"
      description="Se agregara al carrito cuando termine el alta."
      onClose={onClose}
      size="large"
      footer={(
        <>
          <button className="secondary-button" type="button" onClick={onClose}>Cancelar</button>
          <button className="primary-button" type="submit" form="quick-product-form" disabled={submitting || formInvalid}>
            <PackagePlus size={18} />{submitting ? 'Guardando...' : 'Crear producto'}
          </button>
        </>
      )}
    >
      <form id="quick-product-form" className="form-grid form-grid--three direct-number-entry" onSubmit={handleSubmit}>
        <div className="form-field"><label htmlFor="product-code">Codigo *</label><input id="product-code" name="codigo" value={form.codigo} onChange={updateField} maxLength="50" required autoFocus /></div>
        <div className="form-field form-field--span-two"><label htmlFor="product-name">Nombre *</label><input id="product-name" name="nombre" value={form.nombre} onChange={updateField} maxLength="150" required /></div>
        <div className="form-field"><label htmlFor="product-brand">Marca</label><input id="product-brand" name="marca" value={form.marca} onChange={updateField} maxLength="100" /></div>
        <div className="form-field form-field--span-two"><label htmlFor="product-description">Descripcion</label><input id="product-description" name="descripcion" value={form.descripcion} onChange={updateField} maxLength="500" /></div>
        <div className="form-field"><label htmlFor="buy-price">Precio de compra *</label><div className="money-input"><span>$</span><input id="buy-price" name="precioCompra" type="number" min="0" step="0.01" value={form.precioCompra} onChange={updateField} required /></div></div>
        <div className="form-field"><label htmlFor="sale-price">Precio de venta *</label><div className={priceInvalid ? 'money-input money-input--invalid' : 'money-input'}><span>$</span><input id="sale-price" name="precioVenta" type="number" min={Math.max(0.01, purchasePrice)} step="0.01" value={form.precioVenta} onChange={updateField} aria-invalid={priceInvalid} required /></div>{priceInvalid && <small className="field-error">Debe ser igual o mayor al precio de compra.</small>}</div>
        <div className={priceInvalid ? 'margin-preview margin-preview--negative' : 'margin-preview'}><span>Margen unitario</span><strong>{formatMoney(salePrice - purchasePrice)}</strong></div>
        <div className="form-field"><label htmlFor="current-stock">Stock actual *</label><input id="current-stock" name="stockActual" type="number" min="0.001" step="0.001" value={form.stockActual} onChange={updateField} aria-invalid={stockInvalid} required />{stockInvalid && <small className="field-error">Debe ser mayor que cero.</small>}</div>
        <div className="form-field"><label htmlFor="minimum-stock">Stock minimo *</label><input id="minimum-stock" name="stockMinimo" type="number" min="0" step="0.001" value={form.stockMinimo} onChange={updateField} required /></div>
        <div className="form-field"><label htmlFor="product-unit">Unidad *</label><select id="product-unit" name="unidadMedida" value={form.unidadMedida} onChange={updateField}>{units.map((unit) => <option key={unit} value={unit}>{formatLabel(unit)}</option>)}</select></div>
        <div className="form-field form-field--full category-field">
          <label htmlFor="product-category">Categoria *</label>
          <div className="field-with-action">
            <select id="product-category" name="categoriaId" value={form.categoriaId} onChange={updateField} required>
              {!availableCategories.length && <option value="">Sin categorias</option>}
              {availableCategories.map((category) => <option key={category.id} value={category.id}>{category.nombre}</option>)}
            </select>
            <button className="icon-button compact-action" type="button" title="Nueva categoria" aria-label="Nueva categoria" onClick={() => setShowCategoryForm((visible) => !visible)}>{showCategoryForm ? <X size={18} /> : <Plus size={18} />}</button>
          </div>
        </div>

        {showCategoryForm && (
          <div className="inline-category form-field--full">
            <FolderPlus size={19} />
            <div><label htmlFor="category-name">Nueva categoria</label><input id="category-name" value={categoryName} onChange={(event) => setCategoryName(event.target.value)} maxLength="100" /></div>
            <button className="secondary-button" type="button" onClick={createCategory} disabled={categorySubmitting || !categoryName.trim()}>{categorySubmitting ? 'Creando...' : 'Agregar'}</button>
          </div>
        )}
        {error && <p className="form-error form-field--full" role="alert">{error}</p>}
      </form>
    </Modal>
  )
}

function formatLabel(value) {
  return String(value || '').toLowerCase().replaceAll('_', ' ').replace(/^./, (letter) => letter.toUpperCase())
}

function formatMoney(value) {
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(Number(value || 0))
}
