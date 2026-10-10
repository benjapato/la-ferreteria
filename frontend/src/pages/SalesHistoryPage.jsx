import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  AlertTriangle, Ban, ChevronLeft, ChevronRight, Eye, FileText, Filter,
  RotateCcw, Search, ShoppingCart, X,
} from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import { apiRequest } from '../api/client'
import { useAuth } from '../auth/useAuth'
import AppShell from '../components/AppShell'
import CancelSaleModal from '../components/CancelSaleModal'
import SaleDetailModal from '../components/SaleDetailModal'
import SaleReceiptModal from '../components/SaleReceiptModal'

const initialFilters = { number: '', client: '', from: '', to: '', status: '', payment: '' }
const moneyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })
const dateFormatter = new Intl.DateTimeFormat('es-AR', {
  day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
})

export default function SalesHistoryPage() {
  const { user } = useAuth()
  const isAdmin = user.rol === 'ADMINISTRADOR'
  const [searchParams] = useSearchParams()
  const requestedPeriod = searchParams.get('period')
  const pageFilters = useMemo(() => requestedPeriod === 'today'
    ? { ...initialFilters, from: inputDate(new Date()), to: inputDate(new Date()) }
    : initialFilters, [requestedPeriod])
  const [filters, setFilters] = useState(pageFilters)
  const [appliedFilters, setAppliedFilters] = useState(pageFilters)
  const [advancedOpen, setAdvancedOpen] = useState(requestedPeriod === 'today')
  const [history, setHistory] = useState(null)
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState('')
  const [detailSale, setDetailSale] = useState(null)
  const [receiptSale, setReceiptSale] = useState(null)
  const [cancelSale, setCancelSale] = useState(null)
  const [canceling, setCanceling] = useState(false)
  const [error, setError] = useState('')
  const [cancelError, setCancelError] = useState('')

  const loadHistory = useCallback(async (page = 0, values = initialFilters) => {
    setLoading(true)
    setError('')
    const parameters = new URLSearchParams({ page: String(page) })
    Object.entries(values).forEach(([key, value]) => {
      if (value) parameters.set(key, value)
    })
    try {
      setHistory(await apiRequest(`/api/sales/history?${parameters.toString()}`))
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    setFilters(pageFilters)
    setAppliedFilters(pageFilters)
    setAdvancedOpen(requestedPeriod === 'today')
    loadHistory(0, pageFilters)
  }, [loadHistory, pageFilters, requestedPeriod])

  function updateFilter(event) {
    setFilters((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  function applyFilters(event) {
    event.preventDefault()
    const values = { ...filters }
    setAppliedFilters(values)
    loadHistory(0, values)
  }

  function clearFilters() {
    setFilters(initialFilters)
    setAppliedFilters(initialFilters)
    setAdvancedOpen(false)
    loadHistory(0, initialFilters)
  }

  async function openSale(id, target) {
    setActionLoading(`${target}-${id}`)
    setError('')
    try {
      const sale = await apiRequest(`/api/sales/${id}`)
      if (target === 'detail') setDetailSale(sale)
      if (target === 'receipt') setReceiptSale(sale)
      if (target === 'cancel') setCancelSale(sale)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setActionLoading('')
    }
  }

  async function confirmCancellation() {
    setCanceling(true)
    setCancelError('')
    try {
      const canceled = await apiRequest(`/api/sales/${cancelSale.id}/cancel`, { method: 'POST' })
      setHistory((current) => ({ ...current, ventas: current.ventas.map((sale) => sale.id === canceled.id ? { ...sale, estado: canceled.estado } : sale) }))
      setDetailSale((current) => current?.id === canceled.id ? canceled : current)
      setCancelSale(null)
      await loadHistory(history.pagina, appliedFilters)
    } catch (requestError) {
      setCancelError(requestError.message)
    } finally {
      setCanceling(false)
    }
  }

  const hasFilters = Object.values(appliedFilters).some(Boolean)

  return (
    <AppShell>
      <div className="history-page">
        <div className="page-heading">
          <div><p className="eyebrow">Ventas</p><h1>Historial de ventas</h1><p>Consulta comprobantes, detalles y operaciones canceladas.</p></div>
          <Link className="primary-button" to="/ventas/nueva"><ShoppingCart size={18} />Nueva venta</Link>
        </div>

        <form className="history-filters" onSubmit={applyFilters}>
          <div className="history-filters__main">
            <div className="filter-input filter-input--number"><span>#</span><input aria-label="Numero de venta" name="number" type="number" min="1" placeholder="Venta" value={filters.number} onChange={updateFilter} /></div>
            <div className="filter-input"><Search size={17} /><input aria-label="Cliente" name="client" placeholder="Cliente, documento o consumidor final" value={filters.client} onChange={updateFilter} /></div>
            <button className={advancedOpen ? 'secondary-button filter-toggle filter-toggle--active' : 'secondary-button filter-toggle'} type="button" aria-label="Filtros avanzados" aria-expanded={advancedOpen} onClick={() => setAdvancedOpen((open) => !open)}><Filter size={17} /><span>Filtros</span></button>
            <button className="primary-button" type="submit"><Search size={17} />Buscar</button>
            <button className="icon-button reset-filters" type="button" title="Limpiar filtros" aria-label="Limpiar filtros" onClick={clearFilters}><RotateCcw size={18} /></button>
          </div>

          {advancedOpen && (
            <div className="history-filters__advanced">
              <div className="form-field"><label htmlFor="history-from">Fecha desde</label><input id="history-from" name="from" type="date" value={filters.from} onChange={updateFilter} /></div>
              <div className="form-field"><label htmlFor="history-to">Fecha hasta</label><input id="history-to" name="to" type="date" value={filters.to} onChange={updateFilter} /></div>
              <div className="form-field"><label htmlFor="history-status">Estado</label><select id="history-status" name="status" value={filters.status} onChange={updateFilter}><option value="">Todos</option><option value="CONFIRMADA">Confirmada</option><option value="CANCELADA">Cancelada</option></select></div>
              <div className="form-field"><label htmlFor="history-payment">Forma de pago</label><select id="history-payment" name="payment" value={filters.payment} onChange={updateFilter}><option value="">Todas</option><option value="EFECTIVO">Efectivo</option><option value="TRANSFERENCIA">Transferencia</option><option value="TARJETA_DEBITO">Tarjeta de debito</option><option value="TARJETA_CREDITO">Tarjeta de credito</option><option value="COMBINADO">Combinado</option><option value="CUENTA_CORRIENTE">Cuenta corriente</option></select></div>
            </div>
          )}
        </form>

        {error && <div className="sale-error" role="alert"><AlertTriangle size={19} /><span>{error}</span><button className="icon-button" type="button" aria-label="Cerrar error" onClick={() => setError('')}><X size={18} /></button></div>}

        <section className="history-panel">
          <header className="history-panel__header"><div><h2>Ventas registradas</h2><span>{history?.totalElementos || 0} resultados{hasFilters ? ' con los filtros actuales' : ''}</span></div></header>
          {loading ? (
            <div className="history-loading" role="status"><div className="skeleton" /><div className="skeleton" /><div className="skeleton" /></div>
          ) : history?.ventas.length ? (
            <div className="table-scroll">
              <table className="history-table">
                <thead><tr><th>Venta</th><th>Fecha</th><th>Cliente</th><th>Vendedor</th><th>Pago</th><th>Estado</th><th className="align-right">Total</th><th className="align-right">Acciones</th></tr></thead>
                <tbody>{history.ventas.map((sale) => (
                  <tr key={sale.id}>
                    <td><strong>#{sale.id}</strong></td>
                    <td>{dateFormatter.format(new Date(sale.fecha))}</td>
                    <td className="history-client">{sale.cliente}</td>
                    <td>{sale.vendedor}</td>
                    <td><span className="payment-label">{formatLabel(sale.formaPago)}</span></td>
                    <td><span className={sale.estado === 'CANCELADA' ? 'sale-status sale-status--canceled' : 'sale-status'}>{formatLabel(sale.estado)}</span></td>
                    <td className="align-right money-cell">{moneyFormatter.format(Number(sale.total))}</td>
                    <td><div className="history-actions">
                      <button className="icon-button history-action history-action--detail" type="button" title="Ver detalle" aria-label={`Ver detalle de venta ${sale.id}`} disabled={Boolean(actionLoading)} onClick={() => openSale(sale.id, 'detail')}><Eye size={17} /></button>
                      <button className="icon-button history-action" type="button" title="Comprobante" aria-label={`Ver comprobante de venta ${sale.id}`} disabled={Boolean(actionLoading)} onClick={() => openSale(sale.id, 'receipt')}><FileText size={17} /></button>
                      {isAdmin && sale.estado === 'CONFIRMADA' && <button className="icon-button history-action history-action--cancel" type="button" title="Cancelar venta" aria-label={`Cancelar venta ${sale.id}`} disabled={Boolean(actionLoading)} onClick={() => openSale(sale.id, 'cancel')}><Ban size={17} /></button>}
                    </div></td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
          ) : (
            <div className="history-empty"><FileText size={32} /><strong>{hasFilters ? 'No hay ventas que coincidan' : 'Todavia no hay ventas registradas'}</strong><span>{hasFilters ? 'Prueba modificando o limpiando los filtros.' : 'Las ventas confirmadas apareceran en este historial.'}</span>{!hasFilters && <Link className="primary-button" to="/ventas/nueva"><ShoppingCart size={17} />Registrar venta</Link>}</div>
          )}

          {history && history.totalPaginas > 1 && <nav className="history-pagination" aria-label="Paginacion"><button className="icon-button" type="button" aria-label="Pagina anterior" disabled={history.primera || loading} onClick={() => loadHistory(history.pagina - 1, appliedFilters)}><ChevronLeft size={19} /></button><span>Pagina {history.pagina + 1} de {history.totalPaginas}</span><button className="icon-button" type="button" aria-label="Pagina siguiente" disabled={history.ultima || loading} onClick={() => loadHistory(history.pagina + 1, appliedFilters)}><ChevronRight size={19} /></button></nav>}
        </section>
      </div>

      {detailSale && <SaleDetailModal sale={detailSale} isAdmin={isAdmin} onClose={() => setDetailSale(null)} onReceipt={() => { setReceiptSale(detailSale); setDetailSale(null) }} onCancel={() => { setCancelSale(detailSale); setDetailSale(null) }} />}
      {receiptSale && <SaleReceiptModal sale={receiptSale} historical onClose={() => setReceiptSale(null)} />}
      {cancelSale && <CancelSaleModal sale={cancelSale} submitting={canceling} error={cancelError} onClose={() => { setCancelSale(null); setCancelError('') }} onConfirm={confirmCancellation} />}
    </AppShell>
  )
}

function formatLabel(value) {
  return String(value || '').toLowerCase().replaceAll('_', ' ').replace(/^./, (letter) => letter.toUpperCase())
}

function inputDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
