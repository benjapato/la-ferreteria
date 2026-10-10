import { useCallback, useEffect, useState } from 'react'
import {
  AlertTriangle, Banknote, Box, CircleDollarSign, ClipboardList,
  History, LockKeyhole, PackageCheck, RefreshCw, ShoppingCart, TrendingUp,
  UnlockKeyhole,
} from 'lucide-react'
import { apiRequest } from '../api/client'
import { useAuth } from '../auth/useAuth'
import AppShell from '../components/AppShell'
import CashCloseModal from '../components/CashCloseModal'
import CashOpeningModal from '../components/CashOpeningModal'
import DailySalesPayments from '../components/DailySalesPayments'
import { Link } from 'react-router-dom'

const moneyFormatter = new Intl.NumberFormat('es-AR', {
  style: 'currency', currency: 'ARS', minimumFractionDigits: 2,
})
const dateFormatter = new Intl.DateTimeFormat('es-AR', {
  day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
})

const formatMoney = (value) => moneyFormatter.format(Number(value || 0))
const formatDate = (value) => value ? dateFormatter.format(new Date(value)) : '-'
const formatLabel = (value) => String(value || '').toLowerCase().replaceAll('_', ' ').replace(/^./, (letter) => letter.toUpperCase())

function DashboardSkeleton() {
  return (
    <div className="dashboard-skeleton" role="status" aria-label="Cargando tablero">
      <div className="skeleton skeleton--title" />
      <div className="skeleton-grid">
        {Array.from({ length: 5 }, (_, index) => <div className="skeleton skeleton--metric" key={index} />)}
      </div>
      <div className="skeleton skeleton--wide" />
      <div className="skeleton-columns"><div className="skeleton skeleton--panel" /><div className="skeleton skeleton--panel" /></div>
    </div>
  )
}

function Metric({ className, icon, label, value, to }) {
  const content = (
    <>
      <span className="metric__icon">{icon}</span>
      <div><span>{label}</span><strong title={String(value)}>{value}</strong></div>
    </>
  )
  return to ? <Link className={`metric metric--link ${className}`} to={to}>{content}</Link> : <article className={`metric ${className}`}>{content}</article>
}

export default function DashboardPage() {
  const { user } = useAuth()
  const isAdmin = user.rol === 'ADMINISTRADOR'
  const [dashboard, setDashboard] = useState(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState('')
  const [cashModal, setCashModal] = useState(null)

  const loadDashboard = useCallback(async (isRefresh = false) => {
    if (isRefresh) setRefreshing(true)
    else setLoading(true)
    setError('')
    try {
      setDashboard(await apiRequest('/api/dashboard'))
    } catch {
      setError('No se pudo cargar el tablero. Revisa la conexion e intenta nuevamente.')
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }, [])

  useEffect(() => { loadDashboard() }, [loadDashboard])

  async function handleCashChanged() {
    setCashModal(null)
    await loadDashboard(true)
  }

  const cashForClose = dashboard?.caja ? {
    id: dashboard.caja.id,
    arqueo: { efectivoEsperado: dashboard.caja.efectivoEsperado },
  } : null

  const cashForReconciliation = dashboard?.cajaPendienteArqueo ? {
    id: dashboard.cajaPendienteArqueo.id,
    arqueo: { efectivoEsperado: dashboard.cajaPendienteArqueo.efectivoEsperado },
  } : null

  return (
    <AppShell onCashChanged={() => loadDashboard(true)} cashStatusKey={`${dashboard?.caja?.id || ''}:${dashboard?.cajaPendienteArqueo?.id || ''}`}>
      {loading ? <DashboardSkeleton /> : (
        <div className="dashboard">
          <div className="page-heading">
            <div><p className="eyebrow">Resumen operativo</p><h1>Inicio</h1><p>Estado de la ferreteria al dia de hoy.</p></div>
            <div className="page-actions">
              {dashboard?.caja
                ? <Link className="primary-button" to="/ventas/nueva" aria-label="Nueva venta" title="Nueva venta"><ShoppingCart size={18} /><span>Nueva venta</span></Link>
                : <button className="primary-button primary-button--locked" type="button" disabled title="Abri la caja para comenzar a vender"><ShoppingCart size={18} /><span>Nueva venta</span></button>}
              <Link className="secondary-button" to="/ventas" aria-label="Historial de ventas" title="Historial de ventas"><History size={18} /><span>Historial</span></Link>
              <button className="secondary-button" type="button" aria-label="Actualizar tablero" onClick={() => loadDashboard(true)} disabled={refreshing}>
                <RefreshCw className={refreshing ? 'spin' : ''} size={18} /><span>{refreshing ? 'Actualizando...' : 'Actualizar'}</span>
              </button>
            </div>
          </div>

          {error ? (
            <div className="error-state" role="alert">
              <AlertTriangle size={22} />
              <div><strong>No pudimos mostrar la informacion</strong><p>{error}</p></div>
              <button className="secondary-button" type="button" onClick={() => loadDashboard()}>Reintentar</button>
            </div>
          ) : (
            <>
              <section className="metrics-grid" aria-label="Metricas del dia">
                <Metric className="metric--blue" icon={<ShoppingCart size={20} />} label="Ventas de hoy" value={dashboard.metricas.cantidadVentas} to="/ventas?period=today" />
                <Metric className="metric--green" icon={<CircleDollarSign size={20} />} label="Total vendido" value={formatMoney(dashboard.metricas.totalVendido)} to="/ventas?period=today" />
                <Metric className="metric--teal" icon={<TrendingUp size={20} />} label="Ganancia estimada" value={formatMoney(dashboard.metricas.ganancia)} to={isAdmin ? '/reportes' : '/ventas?period=today'} />
                <Metric className="metric--amber" icon={<ClipboardList size={20} />} label="Compras pendientes" value={dashboard.metricas.comprasPendientes} to={isAdmin ? '/compras?status=PENDIENTE' : null} />
                <Metric className="metric--red" icon={<Box size={20} />} label="Productos con stock bajo" value={dashboard.metricas.stockBajo} to="/productos?stock=BAJO" />
              </section>

              <section className={`dashboard-cash-section cash-overview cash-overview--${dashboard.caja ? 'open' : dashboard.cajaPendienteArqueo ? 'pending' : 'closed'}`} aria-labelledby="cash-overview-title">
                <header className="cash-overview__header">
                  <div className="cash-overview__heading">
                    <Banknote size={22} aria-hidden="true" />
                    <h2 id="cash-overview-title">Caja y cobros de hoy</h2>
                    <span className="cash-overview__status">{dashboard.caja ? 'Caja abierta' : dashboard.cajaPendienteArqueo ? 'Arqueo pendiente' : 'Caja cerrada'}</span>
                  </div>
                  <div className="cash-overview__actions">
                    {dashboard.caja ? (
                      <button className="danger-button" type="button" onClick={() => setCashModal('close')}><LockKeyhole size={17} />Cerrar caja</button>
                    ) : dashboard.cajaPendienteArqueo ? (
                      isAdmin
                        ? <button className="primary-button" type="button" onClick={() => setCashModal('reconcile')}><Banknote size={17} />Completar arqueo</button>
                        : <button className="primary-button primary-button--locked" type="button" disabled title="Requiere un administrador"><LockKeyhole size={17} />Requiere administrador</button>
                    ) : (
                      <button className="primary-button" type="button" onClick={() => setCashModal('open')}><UnlockKeyhole size={17} />Abrir caja</button>
                    )}
                    {isAdmin && <Link className="icon-button" to="/caja" title="Ver historial de caja" aria-label="Ver historial de caja"><History size={18} /></Link>}
                  </div>
                </header>
                <div className={`cash-overview__body${dashboard.ventasPorMedio ? '' : ' cash-overview__body--cash-only'}`}>
                  <section className="cash-overview__balance" aria-label="Caja actual">
                    {dashboard.caja ? (
                      <>
                        <dl className="cash-overview__expected"><dt>Efectivo esperado en caja</dt><dd>{formatMoney(dashboard.caja.efectivoEsperado)}</dd></dl>
                        <dl className="cash-overview__details">
                          <div><dt>Apertura</dt><dd>{formatDate(dashboard.caja.fechaApertura)}</dd></div>
                          <div><dt>Total de caja</dt><dd>{formatMoney(dashboard.caja.totalGeneral)}</dd></div>
                        </dl>
                      </>
                    ) : dashboard.cajaPendienteArqueo ? (
                      <>
                        <dl className="cash-overview__expected"><dt>Efectivo pendiente de arqueo</dt><dd>{formatMoney(dashboard.cajaPendienteArqueo.efectivoEsperado)}</dd></dl>
                        <dl className="cash-overview__details">
                          <div><dt>Caja</dt><dd>#{dashboard.cajaPendienteArqueo.id}</dd></div>
                          <div><dt>Cierre automatico</dt><dd>{formatDate(dashboard.cajaPendienteArqueo.fechaCierre)}</dd></div>
                        </dl>
                      </>
                    ) : (
                      <div className="cash-overview__empty"><LockKeyhole size={26} aria-hidden="true" /><strong>Sin caja activa</strong><p>No hay una caja abierta en este momento.</p></div>
                    )}
                  </section>
                  {dashboard.ventasPorMedio && <DailySalesPayments payments={dashboard.ventasPorMedio} />}
                </div>
              </section>

              <div className="dashboard-columns">
                <section className="data-panel">
                  <div className="panel-heading">
                    <div><span className="panel-heading__icon panel-heading__icon--sales"><ShoppingCart size={19} /></span><div><h2>Ventas recientes</h2><p>Ultimos movimientos registrados</p></div></div>
                    <span className="count-badge">{dashboard.ventasRecientes.length}</span>
                  </div>
                  {dashboard.ventasRecientes.length ? (
                    <div className="table-scroll">
                      <table>
                        <thead><tr><th>Venta</th><th>Cliente</th><th>Pago</th><th>Fecha</th><th className="align-right">Total</th></tr></thead>
                        <tbody>{dashboard.ventasRecientes.map((sale) => (
                          <tr key={sale.id}>
                            <td><strong>#{sale.id}</strong></td><td>{sale.cliente}</td>
                            <td><span className="payment-label">{formatLabel(sale.formaPago)}</span></td>
                            <td>{formatDate(sale.fecha)}</td><td className="align-right money-cell">{formatMoney(sale.total)}</td>
                          </tr>
                        ))}</tbody>
                      </table>
                    </div>
                  ) : <div className="empty-state"><PackageCheck size={26} /><strong>Todavia no hay ventas registradas</strong><p>Las nuevas ventas apareceran en esta lista.</p></div>}
                </section>

                <section className="data-panel stock-panel">
                  <div className="panel-heading">
                    <div><span className="panel-heading__icon panel-heading__icon--stock"><AlertTriangle size={19} /></span><div><h2>Stock bajo</h2><p>Productos para reponer</p></div></div>
                    <span className="count-badge count-badge--danger">{dashboard.metricas.stockBajo}</span>
                  </div>
                  {dashboard.productosStockBajo.length ? (
                    <ul className="stock-list">{dashboard.productosStockBajo.map((product) => (
                      <li key={product.id}>
                        <div className="stock-product"><span className="stock-code">{product.codigo || 'S/C'}</span><div><strong>{product.nombre}</strong><span>Minimo: {product.stockMinimo} {formatLabel(product.unidad)}</span></div></div>
                        <div className="stock-amount"><strong>{product.stockActual}</strong><span>{formatLabel(product.unidad)}</span></div>
                      </li>
                    ))}</ul>
                  ) : <div className="empty-state empty-state--success"><PackageCheck size={26} /><strong>Stock en orden</strong><p>No hay productos por debajo del minimo.</p></div>}
                </section>
              </div>
            </>
          )}
        </div>
      )}
      {cashModal === 'open' && <CashOpeningModal endpoint="/api/cash/open" onClose={() => setCashModal(null)} onOpened={handleCashChanged} />}
      {cashModal === 'close' && cashForClose && <CashCloseModal cash={cashForClose} onClose={() => setCashModal(null)} onClosed={handleCashChanged} />}
      {cashModal === 'reconcile' && cashForReconciliation && <CashCloseModal cash={cashForReconciliation} mode="reconcile" onClose={() => setCashModal(null)} onClosed={handleCashChanged} />}
    </AppShell>
  )
}
