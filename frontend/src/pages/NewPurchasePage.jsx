import {
  AlertTriangle, ClipboardCheck, History, PackagePlus, Plus, Search, Trash2,
  Truck, X,
} from 'lucide-react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiRequest } from '../api/client'
import AppShell from '../components/AppShell'
import ProviderFormModal from '../components/ProviderFormModal'
import QuickProductModal from '../components/QuickProductModal'
import { lineSubtotal } from '../utils/quantities'

const moneyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })
const quantityFormatter = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 3 })

export default function NewPurchasePage() {
  const navigate = useNavigate()
  const [setup, setSetup] = useState(null)
  const [catalogSetup, setCatalogSetup] = useState(null)
  const [loading, setLoading] = useState(true)
  const [query, setQuery] = useState('')
  const [supplierId, setSupplierId] = useState('')
  const [lines, setLines] = useState([])
  const [observations, setObservations] = useState('')
  const [modal, setModal] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const loadSetup = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [purchaseData, catalogData] = await Promise.all([
        apiRequest('/api/purchases/setup'),
        apiRequest('/api/catalog/setup'),
      ])
      setSetup(purchaseData)
      setCatalogSetup(catalogData)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadSetup() }, [loadSetup])

  const visibleProducts = useMemo(() => {
    if (!setup) return []
    const normalized = query.trim().toLowerCase()
    if (!normalized) return setup.productos.slice(0, 40)
    return setup.productos.filter((product) => [product.codigo, product.nombre, product.marca, product.categoria]
      .some((value) => String(value || '').toLowerCase().includes(normalized))).slice(0, 40)
  }, [query, setup])

  const total = useMemo(() => lines.reduce((cents, line) => cents + Math.round(lineSubtotal(line.cost, line.quantity) * 100), 0) / 100, [lines])

  const validLines = lines.length > 0 && lines.every((line) => Number(line.quantity) > 0 && Number(line.cost) > 0)
  const canSubmit = Boolean(supplierId) && validLines && total > 0 && !submitting

  function addProduct(product) {
    setLines((current) => {
      const existing = current.find((line) => line.product.id === product.id)
      if (existing) return current.map((line) => line.product.id === product.id ? { ...line, quantity: String(Number(line.quantity || 0) + 1) } : line)
      return [...current, { product, quantity: '1', cost: Number(product.costoActual || 0) > 0 ? String(product.costoActual) : '' }]
    })
  }

  function updateLine(productId, field, value) {
    setLines((current) => current.map((line) => line.product.id === productId ? { ...line, [field]: value } : line))
  }

  function removeLine(productId) {
    setLines((current) => current.filter((line) => line.product.id !== productId))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (!canSubmit) {
      setError('Selecciona un proveedor y revisa las cantidades y costos de la compra.')
      return
    }

    setSubmitting(true)
    setError('')
    try {
      const purchase = await apiRequest('/api/purchases', {
        method: 'POST',
        body: {
          proveedorId: Number(supplierId),
          observaciones: observations.trim(),
          detalles: lines.map((line) => ({ productoId: line.product.id, cantidad: Number(line.quantity), costoUnitario: Number(line.cost) })),
        },
      })
      navigate('/compras', { state: { purchase } })
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setSubmitting(false)
    }
  }

  function handleProviderCreated(provider) {
    setSetup((current) => ({ ...current, proveedores: [...current.proveedores.filter((item) => item.id !== provider.id), provider].sort(sortByName) }))
    setSupplierId(String(provider.id))
    setModal(null)
  }

  async function handleProductCreated(created) {
    try {
      const purchaseData = await apiRequest('/api/purchases/setup')
      setSetup(purchaseData)
      const product = purchaseData.productos.find((item) => item.id === created.id)
      if (product) addProduct(product)
      setQuery('')
      setModal(null)
    } catch (requestError) {
      setError(requestError.message)
    }
  }

  function handleCategoryCreated(category) {
    setCatalogSetup((current) => ({ ...current, categorias: [...current.categorias, category] }))
  }

  if (loading) {
    return <AppShell><div className="purchase-create-loading" role="status"><div className="skeleton skeleton--title" /><div className="purchase-create-loading__grid"><div className="skeleton" /><div className="skeleton" /><div className="skeleton" /></div></div></AppShell>
  }

  return (
    <AppShell>
      <div className="new-purchase-page">
        <div className="page-heading purchases-heading">
          <div><p className="eyebrow">Compras y mercaderia</p><h1>Nueva compra</h1><p>Prepara el pedido; el stock aumentara cuando confirmes la recepcion.</p></div>
          <Link className="secondary-button" to="/compras?tab=compras"><History size={17} /><span>Historial</span></Link>
        </div>

        {error && <div className="sale-error" role="alert"><AlertTriangle size={19} /><span>{error}</span><button className="icon-button" type="button" aria-label="Cerrar error" onClick={() => setError('')}><X size={18} /></button></div>}

        <form className="purchase-workspace" onSubmit={handleSubmit}>
          <section className="purchase-create-panel purchase-product-browser">
            <header className="sale-panel__header"><div><h2>Productos</h2><span>{visibleProducts.length} visibles</span></div><button className="icon-text-button" type="button" onClick={() => setModal('product')}><PackagePlus size={17} />Nuevo</button></header>
            <div className="sale-search purchase-search"><Search size={18} /><input aria-label="Buscar producto para comprar" placeholder="Codigo, nombre, marca o categoria" value={query} onChange={(event) => setQuery(event.target.value)} /></div>
            <div className="purchase-product-results">
              {visibleProducts.length ? visibleProducts.map((product) => <article className="purchase-product" key={product.id}><div className="product-result__code">{product.codigo}</div><div className="product-result__info"><strong>{product.nombre}</strong><span>{product.marca || product.categoria}</span><small>Stock: {quantityFormatter.format(Number(product.stockActual))} {formatLabel(product.unidadMedida)}</small></div><div className="purchase-product__action"><span>Costo actual</span><b>{moneyFormatter.format(Number(product.costoActual))}</b><button className="icon-button add-product" type="button" title="Agregar a la compra" aria-label={`Agregar ${product.nombre}`} onClick={() => addProduct(product)}><Plus size={18} /></button></div></article>) : <div className="compact-empty"><Search size={25} /><strong>No encontramos productos</strong><span>Cambia la busqueda o crea uno nuevo.</span></div>}
            </div>
          </section>

          <section className="purchase-create-panel purchase-lines-panel">
            <header className="sale-panel__header"><div><h2>Productos comprados</h2><span>{lines.length} renglones</span></div>{lines.length > 0 && <button className="text-danger-button" type="button" onClick={() => setLines([])}>Vaciar</button>}</header>
            {lines.length ? <div className="purchase-lines-wrap"><table className="purchase-lines-table"><thead><tr><th>Producto</th><th>Cantidad</th><th>Costo unitario</th><th className="align-right">Subtotal</th><th aria-label="Acciones" /></tr></thead><tbody>{lines.map((line) => <tr key={line.product.id}><td><strong>{line.product.nombre}</strong><small>{line.product.codigo} - {formatLabel(line.product.unidadMedida)}</small></td><td><input aria-label={`Cantidad de ${line.product.nombre}`} type="number" min="0.001" step="0.001" required value={line.quantity} onChange={(event) => updateLine(line.product.id, 'quantity', event.target.value)} /></td><td><div className="money-input purchase-cost-input"><span>$</span><input aria-label={`Costo de ${line.product.nombre}`} type="number" min="0.01" step="0.01" required value={line.cost} onChange={(event) => updateLine(line.product.id, 'cost', event.target.value)} /></div></td><td className="align-right cart-subtotal">{moneyFormatter.format(lineSubtotal(line.cost, line.quantity))}</td><td><button className="icon-button remove-item" type="button" title="Quitar producto" aria-label={`Quitar ${line.product.nombre}`} onClick={() => removeLine(line.product.id)}><Trash2 size={17} /></button></td></tr>)}</tbody></table></div> : <div className="purchase-lines-empty"><ClipboardCheck size={34} /><strong>La compra esta vacia</strong><span>Agrega los productos recibidos desde el panel izquierdo.</span></div>}
            <div className="cart-total"><span>Total estimado</span><strong>{moneyFormatter.format(total)}</strong></div>
          </section>

          <aside className="purchase-create-panel purchase-summary-panel">
            <header className="sale-panel__header"><div><h2>Datos de la compra</h2><span>Se guardara pendiente</span></div></header>
            <div className="purchase-summary-content">
              <div className="form-field"><div className="section-label"><label htmlFor="purchase-supplier">Proveedor *</label><button className="text-action" type="button" onClick={() => setModal('provider')}><Truck size={14} />Nuevo proveedor</button></div><select id="purchase-supplier" value={supplierId} onChange={(event) => setSupplierId(event.target.value)} required><option value="">Seleccionar proveedor</option>{setup.proveedores.map((provider) => <option key={provider.id} value={provider.id}>{provider.nombre}{provider.cuit ? ` - ${provider.cuit}` : ''}</option>)}</select>{!setup.proveedores.length && <span className="field-note">Crea un proveedor para registrar la compra.</span>}</div>
              <div className="form-field"><label htmlFor="purchase-observations">Observaciones</label><textarea id="purchase-observations" rows="5" maxLength="500" placeholder="Numero de factura, condiciones o notas" value={observations} onChange={(event) => setObservations(event.target.value)} /></div>
              <div className="purchase-pending-note"><AlertTriangle size={19} /><div><strong>Recepcion pendiente</strong><span>Guardar no modificara el stock. Podras revisar y confirmar la compra desde el historial.</span></div></div>
            </div>
            <footer className="purchase-summary-footer"><div><span>Total</span><strong>{moneyFormatter.format(total)}</strong></div><button className="confirm-sale-button" type="submit" disabled={!canSubmit}><ClipboardCheck size={19} />{submitting ? 'Registrando...' : 'Registrar compra pendiente'}</button></footer>
          </aside>
        </form>
      </div>

      {modal === 'provider' && <ProviderFormModal onClose={() => setModal(null)} onSaved={handleProviderCreated} />}
      {modal === 'product' && catalogSetup && <QuickProductModal categories={catalogSetup.categorias.filter((category) => category.activa)} units={catalogSetup.unidadesMedida} onClose={() => setModal(null)} onCreated={handleProductCreated} onCategoryCreated={handleCategoryCreated} />}
    </AppShell>
  )
}

function sortByName(first, second) {
  return first.nombre.localeCompare(second.nombre, 'es', { sensitivity: 'base' })
}

function formatLabel(value) {
  return String(value || '').toLowerCase().replaceAll('_', ' ')
}
