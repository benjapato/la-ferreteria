import {
  AlertTriangle, ChevronLeft, ChevronRight, CircleDollarSign, Edit3, Eye,
  HandCoins, Power, PowerOff, RotateCcw, Search, SlidersHorizontal, UserPlus,
  UsersRound, WalletCards, X,
} from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiRequest } from '../api/client'
import AppShell from '../components/AppShell'
import CustomerFormModal from '../components/CustomerFormModal'
import CustomerPaymentModal from '../components/CustomerPaymentModal'
import StatusChangeModal from '../components/StatusChangeModal'

const initialFilters = { query: '', status: 'TODOS', balance: 'TODOS' }
const moneyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })

export default function CustomersPage() {
  const [filters, setFilters] = useState(initialFilters)
  const [appliedFilters, setAppliedFilters] = useState(initialFilters)
  const [customers, setCustomers] = useState(null)
  const [loading, setLoading] = useState(true)
  const [modal, setModal] = useState(null)
  const [statusSubmitting, setStatusSubmitting] = useState(false)
  const [statusError, setStatusError] = useState('')
  const [error, setError] = useState('')

  const loadCustomers = useCallback(async (page = 0, values = initialFilters) => {
    setLoading(true)
    setError('')
    const parameters = new URLSearchParams({ page: String(page) })
    Object.entries(values).forEach(([key, value]) => {
      if (value) parameters.set(key, value)
    })
    try {
      setCustomers(await apiRequest(`/api/customers?${parameters.toString()}`))
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadCustomers() }, [loadCustomers])

  function updateFilter(event) {
    setFilters((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  function applyFilters(event) {
    event.preventDefault()
    const values = { ...filters }
    setAppliedFilters(values)
    loadCustomers(0, values)
  }

  function clearFilters() {
    setFilters(initialFilters)
    setAppliedFilters(initialFilters)
    loadCustomers(0, initialFilters)
  }

  async function reloadCurrentPage() {
    setModal(null)
    await loadCustomers(customers?.pagina || 0, appliedFilters)
  }

  async function confirmStatusChange() {
    setStatusSubmitting(true)
    setStatusError('')
    try {
      await apiRequest(`/api/customers/${modal.customer.id}/status`, { method: 'POST' })
      await reloadCurrentPage()
    } catch (requestError) {
      setStatusError(requestError.message)
    } finally {
      setStatusSubmitting(false)
    }
  }

  const metrics = customers?.metricas || { total: 0, activos: 0, conDeuda: 0, deudaTotal: 0 }

  return (
    <AppShell>
      <div className="customers-page">
        <div className="page-heading customers-heading">
          <div><p className="eyebrow">Cuentas y contactos</p><h1>Clientes</h1><p>Consulta saldos, ventas y cobros desde un solo lugar.</p></div>
          <button className="primary-button" type="button" aria-label="Nuevo cliente" title="Nuevo cliente" onClick={() => setModal({ type: 'form' })}><UserPlus size={18} /><span>Nuevo cliente</span></button>
        </div>

        <div className="customer-metrics">
          <CustomerMetric icon={<UsersRound size={20} />} label="Clientes" value={metrics.total} tone="blue" />
          <CustomerMetric icon={<Power size={20} />} label="Activos" value={metrics.activos} tone="green" />
          <CustomerMetric icon={<WalletCards size={20} />} label="Con deuda" value={metrics.conDeuda} tone="amber" />
          <CustomerMetric icon={<CircleDollarSign size={20} />} label="Deuda total" value={moneyFormatter.format(Number(metrics.deudaTotal))} tone="red" />
        </div>

        <form className="customer-filters" onSubmit={applyFilters}>
          <div className="catalog-search"><Search size={17} /><input name="query" aria-label="Buscar clientes" placeholder="Nombre, documento, telefono o correo" value={filters.query} onChange={updateFilter} /></div>
          <select name="status" aria-label="Filtrar clientes por estado" value={filters.status} onChange={updateFilter}><option value="TODOS">Todos los estados</option><option value="ACTIVOS">Activos</option><option value="INACTIVOS">Inactivos</option></select>
          <select name="balance" aria-label="Filtrar clientes por saldo" value={filters.balance} onChange={updateFilter}><option value="TODOS">Todos los saldos</option><option value="DEUDA">Con deuda</option><option value="AL_DIA">Al dia</option><option value="A_FAVOR">Con saldo a favor</option></select>
          <button className="primary-button" type="submit"><SlidersHorizontal size={17} /><span>Aplicar</span></button>
          <button className="icon-button reset-filters" type="button" title="Limpiar filtros" aria-label="Limpiar filtros" onClick={clearFilters}><RotateCcw size={18} /></button>
        </form>

        {error && <div className="sale-error" role="alert"><AlertTriangle size={19} /><span>{error}</span><button className="icon-button" type="button" aria-label="Cerrar error" onClick={() => setError('')}><X size={18} /></button></div>}

        <section className="customer-panel">
          <header className="catalog-panel__header"><div><h2>Clientes registrados</h2><span>{customers?.totalElementos || 0} resultados</span></div></header>
          {loading ? (
            <div className="history-loading" role="status"><div className="skeleton" /><div className="skeleton" /><div className="skeleton" /></div>
          ) : customers?.clientes.length ? (
            <div className="table-scroll">
              <table className="customers-table">
                <thead><tr><th>Cliente</th><th>Contacto</th><th className="align-right">Saldo</th><th>Cuenta</th><th>Estado</th><th className="align-right">Acciones</th></tr></thead>
                <tbody>{customers.clientes.map((customer) => (
                  <tr key={customer.id} className={customer.activo ? '' : 'catalog-row--inactive'}>
                    <td><div className="catalog-primary"><strong>{customer.nombre}</strong><span>{customer.documento || 'Sin documento'}</span></div></td>
                    <td><div className="catalog-primary"><strong>{customer.telefono || 'Sin telefono'}</strong><span>{customer.email || 'Sin correo'}</span></div></td>
                    <td className={`align-right customer-balance customer-balance--${customer.estadoSaldo.toLowerCase()}`}>{moneyFormatter.format(Number(customer.saldo))}</td>
                    <td><BalanceBadge state={customer.estadoSaldo} /></td>
                    <td><span className={customer.activo ? 'record-status' : 'record-status record-status--inactive'}>{customer.activo ? 'Activo' : 'Inactivo'}</span></td>
                    <td><div className="customer-actions">
                      <Link className="icon-button customer-action--view" to={`/clientes/${customer.id}`} title="Ver ficha" aria-label={`Ver ficha de ${customer.nombre}`}><Eye size={17} /></Link>
                      <button className="icon-button" type="button" title="Editar cliente" aria-label={`Editar ${customer.nombre}`} onClick={() => setModal({ type: 'form', customer })}><Edit3 size={17} /></button>
                      <button className="icon-button customer-action--payment" type="button" title={!customers.cajaAbierta ? 'La caja esta cerrada' : !customer.activo ? 'El cliente esta inactivo' : 'Registrar cobro'} aria-label={`Registrar cobro de ${customer.nombre}`} disabled={!customers.cajaAbierta || !customer.activo} onClick={() => setModal({ type: 'payment', customer })}><HandCoins size={17} /></button>
                      <button className={customer.activo ? 'icon-button customer-action--off' : 'icon-button customer-action--on'} type="button" title={customer.activo ? 'Desactivar cliente' : 'Activar cliente'} aria-label={`${customer.activo ? 'Desactivar' : 'Activar'} ${customer.nombre}`} onClick={() => setModal({ type: 'status', customer })}>{customer.activo ? <PowerOff size={17} /> : <Power size={17} />}</button>
                    </div></td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
          ) : (
            <div className="catalog-empty"><UsersRound size={34} /><strong>No hay clientes para mostrar</strong><span>Modifica los filtros o registra el primer cliente.</span><button className="primary-button" type="button" onClick={() => setModal({ type: 'form' })}><UserPlus size={17} />Nuevo cliente</button></div>
          )}
          {customers && customers.totalPaginas > 1 && <CustomerPagination data={customers} loading={loading} onChange={(page) => loadCustomers(page, appliedFilters)} />}
        </section>
      </div>

      {modal?.type === 'form' && <CustomerFormModal customer={modal.customer} onClose={() => setModal(null)} onSaved={reloadCurrentPage} />}
      {modal?.type === 'payment' && <CustomerPaymentModal customer={modal.customer} cashOpen={customers.cajaAbierta} onClose={() => setModal(null)} onSaved={reloadCurrentPage} />}
      {modal?.type === 'status' && <StatusChangeModal label={modal.customer.nombre} active={modal.customer.activo} submitting={statusSubmitting} error={statusError} onClose={() => { setModal(null); setStatusError('') }} onConfirm={confirmStatusChange} />}
    </AppShell>
  )
}

function CustomerMetric({ icon, label, value, tone }) {
  return <div className={`customer-metric customer-metric--${tone}`}><span>{icon}</span><div><small>{label}</small><strong>{value}</strong></div></div>
}

function BalanceBadge({ state }) {
  const labels = { DEUDA: 'Con deuda', AL_DIA: 'Al dia', A_FAVOR: 'A favor' }
  return <span className={`balance-badge balance-badge--${state.toLowerCase()}`}>{labels[state]}</span>
}

function CustomerPagination({ data, loading, onChange }) {
  return <nav className="history-pagination" aria-label="Paginacion"><button className="icon-button" type="button" aria-label="Pagina anterior" disabled={data.primera || loading} onClick={() => onChange(data.pagina - 1)}><ChevronLeft size={19} /></button><span>Pagina {data.pagina + 1} de {data.totalPaginas}</span><button className="icon-button" type="button" aria-label="Pagina siguiente" disabled={data.ultima || loading} onClick={() => onChange(data.pagina + 1)}><ChevronRight size={19} /></button></nav>
}
