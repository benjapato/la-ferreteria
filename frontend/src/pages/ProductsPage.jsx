import {
  AlertTriangle, Boxes, ChevronLeft, ChevronRight, Edit3,
  FolderTree, PackagePlus, Power, PowerOff, RotateCcw, Search,
  SlidersHorizontal, Percent, X,
} from 'lucide-react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { apiRequest } from '../api/client'
import { useAuth } from '../auth/useAuth'
import AppShell from '../components/AppShell'
import BulkPriceUpdateModal from '../components/BulkPriceUpdateModal'
import CategoryFormModal from '../components/CategoryFormModal'
import InlineStockEditor from '../components/InlineStockEditor'
import ProductFormModal from '../components/ProductFormModal'
import StatusChangeModal from '../components/StatusChangeModal'

const initialFilters = { query: '', category: '', status: 'TODOS', stock: 'TODOS' }
const moneyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })

export default function ProductsPage() {
  const { user } = useAuth()
  const isAdmin = user.rol === 'ADMINISTRADOR'
  const [searchParams] = useSearchParams()
  const requestedStock = searchParams.get('stock')
  const pageFilters = useMemo(() => ['BAJO', 'SIN_STOCK'].includes(requestedStock)
    ? { ...initialFilters, stock: requestedStock }
    : initialFilters, [requestedStock])
  const [activeTab, setActiveTab] = useState('products')
  const [filters, setFilters] = useState(pageFilters)
  const [appliedFilters, setAppliedFilters] = useState(pageFilters)
  const [products, setProducts] = useState(null)
  const [categories, setCategories] = useState([])
  const [units, setUnits] = useState([])
  const [directoryQuery, setDirectoryQuery] = useState('')
  const [loading, setLoading] = useState(true)
  const [modal, setModal] = useState(null)
  const [statusSubmitting, setStatusSubmitting] = useState(false)
  const [statusError, setStatusError] = useState('')
  const [error, setError] = useState('')

  const loadProducts = useCallback(async (page = 0, values = initialFilters, showLoading = true) => {
    if (showLoading) setLoading(true)
    setError('')
    const parameters = new URLSearchParams({ page: String(page) })
    Object.entries(values).forEach(([key, value]) => {
      if (value) parameters.set(key, value)
    })
    try {
      setProducts(await apiRequest(`/api/catalog/products?${parameters.toString()}`))
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      if (showLoading) setLoading(false)
    }
  }, [])

  const loadDirectories = useCallback(async () => {
    try {
      const setup = await apiRequest('/api/catalog/setup')
      setCategories(setup.categorias)
      setUnits(setup.unidadesMedida)
    } catch (requestError) {
      setError(requestError.message)
    }
  }, [])

  useEffect(() => {
    loadDirectories()
  }, [loadDirectories])

  useEffect(() => {
    setActiveTab('products')
    setFilters(pageFilters)
    setAppliedFilters(pageFilters)
    loadProducts(0, pageFilters)
  }, [loadProducts, pageFilters])

  const visibleCategories = useMemo(() => filterDirectory(categories, directoryQuery, ['nombre', 'descripcion']), [categories, directoryQuery])

  function changeTab(tab) {
    setActiveTab(tab)
    setDirectoryQuery('')
    setError('')
  }

  function updateFilter(event) {
    setFilters((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  function applyFilters(event) {
    event.preventDefault()
    const values = { ...filters }
    setAppliedFilters(values)
    loadProducts(0, values)
  }

  function clearFilters() {
    setFilters(initialFilters)
    setAppliedFilters(initialFilters)
    loadProducts(0, initialFilters)
  }

  function filterByStock(stock) {
    const values = {
      ...appliedFilters,
      stock: appliedFilters.stock === stock ? 'TODOS' : stock,
    }
    setFilters(values)
    setAppliedFilters(values)
    loadProducts(0, values)
  }

  function startProductCreation() {
    if (!categories.some((category) => category.activa)) {
      setActiveTab('categories')
      setModal({ type: 'category', returnToProduct: true })
      return
    }
    setModal({ type: 'product' })
  }

  function openPrimaryModal() {
    if (activeTab === 'products') startProductCreation()
    if (activeTab === 'categories') setModal({ type: 'category' })
  }

  async function handleProductSaved() {
    setModal(null)
    await loadProducts(products?.pagina || 0, appliedFilters)
  }

  async function handleInlineStockSaved() {
    await loadProducts(products?.pagina || 0, appliedFilters, false)
  }

  async function handleCategorySaved(saved) {
    const returningToProduct = modal?.returnToProduct
    setCategories((current) => [...current.filter((item) => item.id !== saved.id), saved].sort(sortByName))
    await loadProducts(products?.pagina || 0, appliedFilters)
    setModal(returningToProduct ? { type: 'product' } : null)
    if (returningToProduct) setActiveTab('products')
  }

  async function confirmStatusChange() {
    setStatusSubmitting(true)
    setStatusError('')
    try {
      const paths = {
        product: `/api/catalog/products/${modal.item.id}/status`,
        category: `/api/catalog/categories/${modal.item.id}/status`,
      }
      const updated = await apiRequest(paths[modal.kind], { method: 'POST' })
      if (modal.kind === 'product') await loadProducts(products?.pagina || 0, appliedFilters)
      if (modal.kind === 'category') {
        setCategories((current) => current.map((item) => item.id === updated.id ? updated : item))
        await loadProducts(products?.pagina || 0, appliedFilters)
      }
      setModal(null)
    } catch (requestError) {
      setStatusError(requestError.message)
    } finally {
      setStatusSubmitting(false)
    }
  }

  const modalItemActive = modal?.kind === 'category' ? modal?.item?.activa : modal?.item?.activo

  return (
    <AppShell>
      <div className="catalog-page">
        <div className="page-heading catalog-heading">
          <div><p className="eyebrow">Mercaderia</p><h1>Catalogo</h1><p>Administra productos, categorias y niveles de stock.</p></div>
          {isAdmin && <div className="page-actions">{activeTab === 'products' && <button className="secondary-button" type="button" aria-label="Actualizar precios" title="Actualizar precios" onClick={() => setModal({ type: 'prices' })}><Percent size={18} /><span>Actualizar precios</span></button>}<button className="primary-button" type="button" aria-label={activeTab === 'products' ? 'Nuevo producto' : 'Nueva categoria'} title={activeTab === 'products' ? 'Nuevo producto' : 'Nueva categoria'} onClick={openPrimaryModal}>{activeTab === 'products' ? <PackagePlus size={18} /> : <FolderTree size={18} />}<span>{activeTab === 'products' ? 'Nuevo producto' : 'Nueva categoria'}</span></button></div>}
        </div>

        <div className="catalog-tabs" role="tablist" aria-label="Secciones del catalogo">
          <button className={activeTab === 'products' ? 'catalog-tab catalog-tab--active' : 'catalog-tab'} type="button" role="tab" aria-selected={activeTab === 'products'} onClick={() => changeTab('products')}><Boxes size={18} />Productos</button>
          <button className={activeTab === 'categories' ? 'catalog-tab catalog-tab--active' : 'catalog-tab'} type="button" role="tab" aria-selected={activeTab === 'categories'} onClick={() => changeTab('categories')}><FolderTree size={18} />Categorias</button>
        </div>

        {error && <div className="sale-error" role="alert"><AlertTriangle size={19} /><span>{error}</span><button className="icon-button" type="button" aria-label="Cerrar error" onClick={() => setError('')}><X size={18} /></button></div>}

        {activeTab === 'products' && (
          <ProductsView
            products={products}
            categories={categories}
            filters={filters}
            loading={loading}
            isAdmin={isAdmin}
            onFilterChange={updateFilter}
            onApplyFilters={applyFilters}
            onClearFilters={clearFilters}
            onStockFilter={filterByStock}
            appliedStock={appliedFilters.stock}
            onPageChange={(page) => loadProducts(page, appliedFilters)}
            onCreate={startProductCreation}
            onEdit={(product) => setModal({ type: 'product', item: product })}
            onStockSaved={handleInlineStockSaved}
            onStatus={(product) => setModal({ type: 'status', kind: 'product', item: product })}
          />
        )}

        {activeTab === 'categories' && <DirectoryView items={visibleCategories} query={directoryQuery} isAdmin={isAdmin} onQueryChange={setDirectoryQuery} onCreate={() => setModal({ type: 'category' })} onEdit={(item) => setModal({ type: 'category', item })} onStatus={(item) => setModal({ type: 'status', kind: 'category', item })} />}
      </div>

      {modal?.type === 'product' && <ProductFormModal product={modal.item} categories={categories} units={units} onClose={() => setModal(null)} onSaved={handleProductSaved} />}
      {modal?.type === 'category' && <CategoryFormModal category={modal.item} title={modal.returnToProduct ? 'Primero crea una categoria' : undefined} description={modal.returnToProduct ? 'Todo producto necesita una categoria. Al guardarla, continuaremos con el alta del producto.' : undefined} onClose={() => setModal(null)} onSaved={handleCategorySaved} />}
      {modal?.type === 'prices' && <BulkPriceUpdateModal totalProducts={products?.metricas.total || 0} onClose={() => setModal(null)} onUpdated={handleProductSaved} />}
      {modal?.type === 'status' && <StatusChangeModal label={modal.item.nombre} active={modalItemActive} submitting={statusSubmitting} error={statusError} onClose={() => { setModal(null); setStatusError('') }} onConfirm={confirmStatusChange} />}
    </AppShell>
  )
}

function ProductsView({ products, categories, filters, loading, isAdmin, onFilterChange, onApplyFilters, onClearFilters, onStockFilter, appliedStock, onPageChange, onCreate, onEdit, onStockSaved, onStatus }) {
  const metrics = products?.metricas || { total: 0, activos: 0, stockBajo: 0, sinStock: 0 }
  const panelTitle = appliedStock === 'BAJO'
    ? 'Productos con stock bajo'
    : appliedStock === 'SIN_STOCK'
      ? 'Productos sin stock'
      : 'Productos registrados'
  return (
    <>
      <div className="catalog-metrics">
        <CatalogMetric icon={<Boxes size={20} />} label="Productos" value={metrics.total} tone="blue" />
        <CatalogMetric icon={<Power size={20} />} label="Activos" value={metrics.activos} tone="green" />
        <CatalogMetric icon={<AlertTriangle size={20} />} label="Stock bajo" value={metrics.stockBajo} tone="amber" active={appliedStock === 'BAJO'} onClick={() => onStockFilter('BAJO')} />
        <CatalogMetric icon={<PowerOff size={20} />} label="Sin stock" value={metrics.sinStock} tone="red" active={appliedStock === 'SIN_STOCK'} onClick={() => onStockFilter('SIN_STOCK')} />
      </div>

      <form className="catalog-filters" onSubmit={onApplyFilters}>
        <div className="catalog-search"><Search size={17} /><input name="query" aria-label="Buscar productos" placeholder="Codigo, producto, marca o categoria" value={filters.query} onChange={onFilterChange} /></div>
        <select name="category" aria-label="Filtrar por categoria" value={filters.category} onChange={onFilterChange}><option value="">Todas las categorias</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.nombre}</option>)}</select>
        <select name="status" aria-label="Filtrar por estado" value={filters.status} onChange={onFilterChange}><option value="TODOS">Todos los estados</option><option value="ACTIVOS">Activos</option><option value="INACTIVOS">Inactivos</option></select>
        <select name="stock" aria-label="Filtrar por stock" value={filters.stock} onChange={onFilterChange}><option value="TODOS">Todo el stock</option><option value="BAJO">Stock bajo</option><option value="SIN_STOCK">Sin stock</option></select>
        <button className="primary-button" type="submit"><SlidersHorizontal size={17} /><span>Aplicar</span></button>
        <button className="icon-button reset-filters" type="button" title="Limpiar filtros" aria-label="Limpiar filtros" onClick={onClearFilters}><RotateCcw size={18} /></button>
      </form>

      <section className="catalog-panel">
        <header className="catalog-panel__header"><div><h2>{panelTitle}</h2><span>{products?.totalElementos || 0} resultados</span></div></header>
        {loading ? <CatalogLoading /> : products?.productos.length ? (
          <div className="table-scroll">
            <table className="products-table">
              <thead><tr><th>Codigo</th><th>Producto</th><th>Categoria</th><th>Stock</th><th className="align-right">Compra</th><th className="align-right">Venta</th><th className="align-right">Margen</th><th>Estado</th>{isAdmin && <th className="align-right">Acciones</th>}</tr></thead>
              <tbody>{products.productos.map((product) => <ProductRow key={product.id} product={product} isAdmin={isAdmin} onEdit={onEdit} onStockSaved={onStockSaved} onStatus={onStatus} />)}</tbody>
            </table>
          </div>
        ) : <CatalogEmpty icon={<Boxes size={34} />} title="No hay productos para mostrar" description="Modifica los filtros o registra el primer producto." action={isAdmin ? <button className="primary-button" type="button" onClick={onCreate}><PackagePlus size={17} />Nuevo producto</button> : null} />}
        {products && products.totalPaginas > 1 && <Pagination page={products.pagina} totalPages={products.totalPaginas} first={products.primera} last={products.ultima} disabled={loading} onChange={onPageChange} />}
      </section>
    </>
  )
}

function ProductRow({ product, isAdmin, onEdit, onStockSaved, onStatus }) {
  return (
    <tr className={product.activo ? '' : 'catalog-row--inactive'}>
      <td><span className="product-code">{product.codigo}</span></td>
      <td><div className="catalog-primary"><strong>{product.nombre}</strong><span>{product.marca || 'Sin marca'}</span></div></td>
      <td>{product.categoria}</td>
      <td><InlineStockEditor product={product} editable={isAdmin && product.activo} onSaved={onStockSaved} /></td>
      <td className="align-right">{moneyFormatter.format(Number(product.precioCompra))}</td>
      <td className="align-right money-cell">{moneyFormatter.format(Number(product.precioVenta))}</td>
      <td className="align-right"><div className="margin-cell"><strong>{moneyFormatter.format(Number(product.margen))}</strong><span>{product.margenPorcentaje == null ? 'Sin base' : `${Number(product.margenPorcentaje).toFixed(1)}%`}</span></div></td>
      <td><span className={product.activo ? 'record-status' : 'record-status record-status--inactive'}>{product.activo ? 'Activo' : 'Inactivo'}</span></td>
      {isAdmin && <td><div className="catalog-actions"><button className="icon-button" type="button" title="Editar producto" aria-label={`Editar ${product.nombre}`} onClick={() => onEdit(product)}><Edit3 size={17} /></button><button className={product.activo ? 'icon-button catalog-action--off' : 'icon-button catalog-action--on'} type="button" title={product.activo ? 'Desactivar producto' : 'Activar producto'} aria-label={`${product.activo ? 'Desactivar' : 'Activar'} ${product.nombre}`} onClick={() => onStatus(product)}>{product.activo ? <PowerOff size={17} /> : <Power size={17} />}</button></div></td>}
    </tr>
  )
}

function DirectoryView({ items, query, isAdmin, onQueryChange, onCreate, onEdit, onStatus }) {
  return (
    <section className="catalog-panel directory-panel">
      <header className="directory-toolbar"><div><h2>Categorias</h2><span>{items.length} registros visibles</span></div><div className="catalog-search"><Search size={17} /><input aria-label="Buscar categorias" placeholder="Buscar por nombre o descripcion" value={query} onChange={(event) => onQueryChange(event.target.value)} /></div></header>
      {items.length ? <div className="table-scroll"><table className="directory-table"><thead><tr><th>Categoria</th><th>Descripcion</th><th>Estado</th>{isAdmin && <th className="align-right">Acciones</th>}</tr></thead><tbody>{items.map((item) => <CategoryRow key={item.id} item={item} isAdmin={isAdmin} onEdit={onEdit} onStatus={onStatus} />)}</tbody></table></div> : <CatalogEmpty icon={<FolderTree size={34} />} title={query ? 'No hay coincidencias' : 'Todavia no hay categorias'} description={query ? 'Prueba con otra busqueda.' : 'Crea la primera categoria para comenzar.'} action={isAdmin && !query ? <button className="primary-button" type="button" onClick={onCreate}><FolderTree size={17} />Crear categoria</button> : null} />}
    </section>
  )
}

function CategoryRow({ item, isAdmin, onEdit, onStatus }) {
  return <tr className={item.activa ? '' : 'catalog-row--inactive'}><td><div className="catalog-primary"><strong>{item.nombre}</strong></div></td><td className="directory-description">{item.descripcion || 'Sin descripcion'}</td><td><span className={item.activa ? 'record-status' : 'record-status record-status--inactive'}>{item.activa ? 'Activa' : 'Inactiva'}</span></td>{isAdmin && <td><div className="catalog-actions"><button className="icon-button" type="button" title="Editar categoria" aria-label={`Editar ${item.nombre}`} onClick={() => onEdit(item)}><Edit3 size={17} /></button><button className={item.activa ? 'icon-button catalog-action--off' : 'icon-button catalog-action--on'} type="button" title={item.activa ? 'Desactivar categoria' : 'Activar categoria'} aria-label={`${item.activa ? 'Desactivar' : 'Activar'} ${item.nombre}`} onClick={() => onStatus(item)}>{item.activa ? <PowerOff size={17} /> : <Power size={17} />}</button></div></td>}</tr>
}

function CatalogMetric({ icon, label, value, tone, active = false, onClick }) {
  const Component = onClick ? 'button' : 'div'
  const interactiveProps = onClick ? {
    type: 'button',
    className: `catalog-metric catalog-metric--${tone} catalog-metric--interactive${active ? ' catalog-metric--active' : ''}`,
    'aria-label': `${active ? 'Quitar filtro' : 'Mostrar'} ${label.toLowerCase()}`,
    'aria-pressed': active,
    title: active ? `Quitar filtro ${label.toLowerCase()}` : `Mostrar ${label.toLowerCase()}`,
    onClick,
  } : {
    className: `catalog-metric catalog-metric--${tone}`,
  }
  return <Component {...interactiveProps}><span>{icon}</span><div><small>{label}</small><strong>{value}</strong></div></Component>
}

function CatalogLoading() {
  return <div className="history-loading" role="status"><div className="skeleton" /><div className="skeleton" /><div className="skeleton" /><div className="skeleton" /></div>
}

function CatalogEmpty({ icon, title, description, action }) {
  return <div className="catalog-empty">{icon}<strong>{title}</strong><span>{description}</span>{action}</div>
}

function Pagination({ page, totalPages, first, last, disabled, onChange }) {
  return <nav className="history-pagination" aria-label="Paginacion"><button className="icon-button" type="button" aria-label="Pagina anterior" disabled={first || disabled} onClick={() => onChange(page - 1)}><ChevronLeft size={19} /></button><span>Pagina {page + 1} de {totalPages}</span><button className="icon-button" type="button" aria-label="Pagina siguiente" disabled={last || disabled} onClick={() => onChange(page + 1)}><ChevronRight size={19} /></button></nav>
}

function filterDirectory(items, query, fields) {
  const normalized = query.trim().toLowerCase()
  if (!normalized) return items
  return items.filter((item) => fields.some((field) => String(item[field] || '').toLowerCase().includes(normalized)))
}

function sortByName(first, second) {
  return first.nombre.localeCompare(second.nombre, 'es', { sensitivity: 'base' })
}
