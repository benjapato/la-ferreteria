import {
  AlertTriangle, Ban, ChevronLeft, ChevronRight, CircleDollarSign, ClipboardList,
  Eye, Filter, PackageCheck, Plus, RotateCcw, Search, Timer, Truck, X,
} from 'lucide-react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { apiRequest } from '../api/client'
import AppShell from '../components/AppShell'
import ProvidersPanel from '../components/ProvidersPanel'
import PurchaseActionModal from '../components/PurchaseActionModal'
import PurchaseDetailModal from '../components/PurchaseDetailModal'

const initialFilters = { number: '', supplier: '', from: '', to: '', status: '' }
const moneyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })
const dateFormatter = new Intl.DateTimeFormat('es-AR', {
  day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
})

export default function PurchasesPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const requestedStatus = searchParams.get('status')
  const requestedTab = searchParams.get('tab')
  const pageFilters = useMemo(() => requestedStatus === 'PENDIENTE'
    ? { ...initialFilters, status: 'PENDIENTE' }
    : initialFilters, [requestedStatus])
  const [filters, setFilters] = useState(pageFilters)
  const [activeTab, setActiveTab] = useState(
    location.state?.purchase || requestedStatus || requestedTab === 'compras'
      ? 'purchases'
      : 'providers',
  )
  const [appliedFilters, setAppliedFilters] = useState(pageFilters)
  const [advancedOpen, setAdvancedOpen] = useState(requestedStatus === 'PENDIENTE')
  const [purchases, setPurchases] = useState(null)
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [modal, setModal] = useState(() => location.state?.purchase ? { type: 'detail', purchase: location.state.purchase } : null)
  const [error, setError] = useState('')
  const [modalError, setModalError] = useState('')

  const loadPurchases = useCallback(async (page = 0, values = initialFilters) => {
    setLoading(true)
    setError('')
    const parameters = new URLSearchParams({ page: String(page) })
    Object.entries(values).forEach(([key, value]) => {
      if (value) parameters.set(key, value)
    })
    try {
      setPurchases(await apiRequest(`/api/purchases?${parameters.toString()}`))
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (requestedStatus || requestedTab === 'compras') setActiveTab('purchases')
    if (requestedTab === 'proveedores') setActiveTab('providers')
    setFilters(pageFilters)
    setAppliedFilters(pageFilters)
    setAdvancedOpen(requestedStatus === 'PENDIENTE')
    loadPurchases(0, pageFilters)
  }, [loadPurchases, pageFilters, requestedStatus, requestedTab])

  useEffect(() => {
    if (location.state?.purchase) {
      setActiveTab('purchases')
      navigate(location.pathname, { replace: true, state: null })
    }
  }, [location.pathname, location.state, navigate])

  function updateFilter(event) {
    setFilters((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  function applyFilters(event) {
    event.preventDefault()
    const values = { ...filters }
    setAppliedFilters(values)
    loadPurchases(0, values)
  }

  function clearFilters() {
    setFilters(initialFilters)
    setAppliedFilters(initialFilters)
    setAdvancedOpen(false)
    loadPurchases(0, initialFilters)
  }

  async function openPurchase(id, type = 'detail') {
    setActionLoading(`${type}-${id}`)
    setError('')
    setModalError('')
    try {
      const purchase = await apiRequest(`/api/purchases/${id}`)
      setModal({ type, purchase })
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setActionLoading('')
    }
  }

  async function confirmAction() {
    setSubmitting(true)
    setModalError('')
    try {
      await apiRequest(`/api/purchases/${modal.purchase.id}/${modal.type === 'confirm' ? 'confirm' : 'cancel'}`, { method: 'POST' })
      setModal(null)
      await loadPurchases(purchases?.pagina || 0, appliedFilters)
    } catch (requestError) {
      setModalError(requestError.message)
    } finally {
      setSubmitting(false)
    }
  }

  const metrics = purchases?.metricas || { total: 0, pendientes: 0, confirmadas: 0, invertido: 0 }
  const hasFilters = Object.values(appliedFilters).some(Boolean)

  return (
    <AppShell>
      <div className="purchases-page">
        <div className="page-heading purchases-heading">
          <div><p className="eyebrow">Abastecimiento</p><h1>Proveedores</h1><p>Administra tus proveedores, registra compras y controla la reposicion.</p></div>
          {activeTab === 'purchases' && <Link className="primary-button" to="/compras/nueva" aria-label="Nueva compra" title="Nueva compra"><Plus size={18} /><span>Nueva compra</span></Link>}
        </div>

        <div className="catalog-tabs purchase-section-tabs" role="tablist" aria-label="Proveedores y compras">
          <button className={activeTab === 'providers' ? 'catalog-tab catalog-tab--active' : 'catalog-tab'} type="button" role="tab" aria-selected={activeTab === 'providers'} onClick={() => setActiveTab('providers')}><Truck size={18} />Proveedores</button>
          <button className={activeTab === 'purchases' ? 'catalog-tab catalog-tab--active' : 'catalog-tab'} type="button" role="tab" aria-selected={activeTab === 'purchases'} onClick={() => setActiveTab('purchases')}><ClipboardList size={18} />Compras</button>
        </div>

        {activeTab === 'providers' && <ProvidersPanel />}

        {activeTab === 'purchases' && <>
          <div className="purchase-metrics">
            <PurchaseMetric icon={<ClipboardList size={20} />} label="Compras" value={metrics.total} tone="blue" />
            <PurchaseMetric icon={<Timer size={20} />} label="Pendientes" value={metrics.pendientes} tone="amber" />
            <PurchaseMetric icon={<PackageCheck size={20} />} label="Confirmadas" value={metrics.confirmadas} tone="green" />
            <PurchaseMetric icon={<CircleDollarSign size={20} />} label="Total recibido" value={moneyFormatter.format(Number(metrics.invertido))} tone="red" />
          </div>

          <form className="purchase-filters" onSubmit={applyFilters}>
            <div className="purchase-filters__main">
              <div className="filter-input filter-input--number"><span>#</span><input aria-label="Numero de compra" name="number" type="number" min="1" placeholder="Compra" value={filters.number} onChange={updateFilter} /></div>
              <div className="filter-input"><Search size={17} /><input aria-label="Proveedor" name="supplier" placeholder="Nombre del proveedor" value={filters.supplier} onChange={updateFilter} /></div>
              <button className={advancedOpen ? 'secondary-button filter-toggle filter-toggle--active' : 'secondary-button filter-toggle'} type="button" aria-label="Filtros avanzados" aria-expanded={advancedOpen} onClick={() => setAdvancedOpen((open) => !open)}><Filter size={17} /><span>Filtros</span></button>
              <button className="primary-button" type="submit"><Search size={17} />Buscar</button>
              <button className="icon-button reset-filters" type="button" title="Limpiar filtros" aria-label="Limpiar filtros" onClick={clearFilters}><RotateCcw size={18} /></button>
            </div>
            {advancedOpen && <div className="purchase-filters__advanced"><div className="form-field"><label htmlFor="purchase-from">Fecha desde</label><input id="purchase-from" name="from" type="date" value={filters.from} onChange={updateFilter} /></div><div className="form-field"><label htmlFor="purchase-to">Fecha hasta</label><input id="purchase-to" name="to" type="date" value={filters.to} onChange={updateFilter} /></div><div className="form-field"><label htmlFor="purchase-status">Estado</label><select id="purchase-status" name="status" value={filters.status} onChange={updateFilter}><option value="">Todos</option><option value="PENDIENTE">Pendiente</option><option value="CONFIRMADA">Confirmada</option><option value="CANCELADA">Cancelada</option></select></div></div>}
          </form>

          {error && <div className="sale-error" role="alert"><AlertTriangle size={19} /><span>{error}</span><button className="icon-button" type="button" aria-label="Cerrar error" onClick={() => setError('')}><X size={18} /></button></div>}

          <section className="purchase-panel">
            <header className="history-panel__header"><div><h2>Compras registradas</h2><span>{purchases?.totalElementos || 0} resultados{hasFilters ? ' con los filtros actuales' : ''}</span></div></header>
            {loading ? <PurchaseLoading /> : purchases?.compras.length ? <div className="table-scroll"><table className="purchases-table"><thead><tr><th>Compra</th><th>Fecha</th><th>Proveedor</th><th>Registrada por</th><th>Estado</th><th className="align-right">Total</th><th className="align-right">Acciones</th></tr></thead><tbody>{purchases.compras.map((purchase) => <tr key={purchase.id}><td><strong>#{purchase.id}</strong></td><td>{dateFormatter.format(new Date(purchase.fecha))}</td><td><div className="catalog-primary"><strong>{purchase.proveedor}</strong><span>Proveedor</span></div></td><td>{purchase.usuario}</td><td><PurchaseStatus status={purchase.estado} /></td><td className="align-right money-cell">{moneyFormatter.format(Number(purchase.total))}</td><td><div className="purchase-actions"><button className="icon-button purchase-action--view" type="button" title="Ver detalle" aria-label={`Ver compra ${purchase.id}`} disabled={Boolean(actionLoading)} onClick={() => openPurchase(purchase.id)}><Eye size={17} /></button>{purchase.estado === 'PENDIENTE' && <button className="icon-button purchase-action--confirm" type="button" title="Confirmar recepcion" aria-label={`Confirmar compra ${purchase.id}`} disabled={Boolean(actionLoading)} onClick={() => openPurchase(purchase.id, 'confirm')}><PackageCheck size={17} /></button>}{purchase.estado === 'PENDIENTE' && <button className="icon-button purchase-action--cancel" type="button" title="Cancelar compra" aria-label={`Cancelar compra ${purchase.id}`} disabled={Boolean(actionLoading)} onClick={() => openPurchase(purchase.id, 'cancel')}><Ban size={17} /></button>}</div></td></tr>)}</tbody></table></div> : <div className="history-empty"><Truck size={34} /><strong>{hasFilters ? 'No hay compras que coincidan' : 'Todavia no hay compras registradas'}</strong><span>{hasFilters ? 'Prueba modificando o limpiando los filtros.' : 'Registra un pedido para comenzar a controlar la reposicion.'}</span>{!hasFilters && <Link className="primary-button" to="/compras/nueva"><Plus size={17} />Nueva compra</Link>}</div>}
            {purchases && purchases.totalPaginas > 1 && <Pagination data={purchases} loading={loading} onChange={(page) => loadPurchases(page, appliedFilters)} />}
          </section>
        </>}
      </div>

      {modal?.type === 'detail' && <PurchaseDetailModal purchase={modal.purchase} onClose={() => setModal(null)} onConfirm={() => setModal({ type: 'confirm', purchase: modal.purchase })} onCancel={() => setModal({ type: 'cancel', purchase: modal.purchase })} />}
      {(modal?.type === 'confirm' || modal?.type === 'cancel') && <PurchaseActionModal purchase={modal.purchase} action={modal.type} submitting={submitting} error={modalError} onClose={() => { setModal(null); setModalError('') }} onConfirm={confirmAction} />}
    </AppShell>
  )
}

function PurchaseMetric({ icon, label, value, tone }) {
  return <div className={`purchase-metric purchase-metric--${tone}`}><span>{icon}</span><div><small>{label}</small><strong>{value}</strong></div></div>
}

function PurchaseStatus({ status }) {
  return <span className={`purchase-status purchase-status--${status.toLowerCase()}`}>{status === 'PENDIENTE' ? 'Pendiente' : status === 'CONFIRMADA' ? 'Confirmada' : 'Cancelada'}</span>
}

function PurchaseLoading() {
  return <div className="history-loading" role="status"><div className="skeleton" /><div className="skeleton" /><div className="skeleton" /></div>
}

function Pagination({ data, loading, onChange }) {
  return <nav className="history-pagination" aria-label="Paginacion"><button className="icon-button" type="button" aria-label="Pagina anterior" disabled={data.primera || loading} onClick={() => onChange(data.pagina - 1)}><ChevronLeft size={19} /></button><span>Pagina {data.pagina + 1} de {data.totalPaginas}</span><button className="icon-button" type="button" aria-label="Pagina siguiente" disabled={data.ultima || loading} onClick={() => onChange(data.pagina + 1)}><ChevronRight size={19} /></button></nav>
}
