import { useEffect, useState } from 'react'
import {
  AlertTriangle, Banknote, BarChart3, CalendarDays, CircleDollarSign,
  PackageOpen, Printer, ReceiptText, RefreshCw, TrendingUp, Trophy, WalletCards,
} from 'lucide-react'
import { apiRequest } from '../api/client'
import AppShell from '../components/AppShell'
import { printDocument } from '../utils/printDocument'

const moneyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', minimumFractionDigits: 2 })
const numberFormatter = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 3 })
const dateFormatter = new Intl.DateTimeFormat('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })

function inputDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const today = new Date()
const todayValue = inputDate(today)
const monthStartValue = inputDate(new Date(today.getFullYear(), today.getMonth(), 1))
const initialPeriods = {
  sales: { from: todayValue, to: todayValue },
  products: { from: monthStartValue, to: todayValue },
  cash: { from: monthStartValue, to: todayValue },
}

const reportTabs = [
  { id: 'sales', label: 'Ventas', icon: BarChart3 },
  { id: 'products', label: 'Mas vendidos', icon: Trophy },
  { id: 'cash', label: 'Caja', icon: WalletCards },
]

const formatMoney = (value) => moneyFormatter.format(Number(value || 0))
const formatNumber = (value) => numberFormatter.format(Number(value || 0))
const formatDate = (value) => value ? dateFormatter.format(new Date(value)) : '-'
const formatLabel = (value) => String(value || '').toLowerCase().replaceAll('_', ' ').replace(/^./, (letter) => letter.toUpperCase())

export default function ReportsPage() {
  const [active, setActive] = useState('sales')
  const [periods, setPeriods] = useState(initialPeriods)
  const [reports, setReports] = useState({})
  const [loading, setLoading] = useState('sales')
  const [error, setError] = useState('')

  useEffect(() => {
    loadReport('sales', initialPeriods.sales)
  }, [])

  async function loadReport(type, period = periods[type]) {
    setLoading(type)
    setError('')
    const query = new URLSearchParams(period)
    try {
      const report = await apiRequest(`/api/reports/${type}?${query}`)
      setReports((current) => ({ ...current, [type]: report }))
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading('')
    }
  }

  function selectTab(type) {
    setActive(type)
    setError('')
    if (!reports[type]) loadReport(type, periods[type])
  }

  function updatePeriod(event) {
    setPeriods((current) => ({
      ...current,
      [active]: { ...current[active], [event.target.name]: event.target.value },
    }))
  }

  function submitPeriod(event) {
    event.preventDefault()
    loadReport(active, periods[active])
  }

  const report = reports[active]
  const activeLabel = reportTabs.find((tab) => tab.id === active)?.label || 'Reporte'

  return (
    <AppShell>
      <div className="reports-page">
        <header className="report-print-heading">
          <strong>La Ferreteria</strong>
          <h1>Reporte de {activeLabel.toLowerCase()}</h1>
          <p>Periodo: {formatInputDate(periods[active].from)} al {formatInputDate(periods[active].to)}</p>
        </header>
        <div className="page-heading report-heading">
          <div><p className="eyebrow">Analisis del negocio</p><h1>Reportes</h1><p>Ventas, rentabilidad e ingresos de caja en un solo lugar.</p></div>
          <button className="secondary-button print-button" type="button" aria-label="Imprimir reporte" title="Imprimir reporte" onClick={() => printDocument('printing-report')} disabled={!report}><Printer size={18} /><span>Imprimir</span></button>
        </div>

        <div className="report-tabs" role="tablist" aria-label="Tipos de reporte">
          {reportTabs.map(({ id, label, icon: Icon }) => <button key={id} className={active === id ? 'report-tab report-tab--active' : 'report-tab'} type="button" role="tab" aria-selected={active === id} onClick={() => selectTab(id)}><Icon size={18} /><span>{label}</span></button>)}
        </div>

        <form className="report-period" onSubmit={submitPeriod}>
          <div className="report-period__title"><CalendarDays size={19} /><div><strong>Periodo del reporte</strong><span>Selecciona las fechas que queres analizar.</span></div></div>
          <div className="form-field"><label htmlFor="report-from">Desde</label><input id="report-from" name="from" type="date" value={periods[active].from} onChange={updatePeriod} required /></div>
          <div className="form-field"><label htmlFor="report-to">Hasta</label><input id="report-to" name="to" type="date" value={periods[active].to} onChange={updatePeriod} required /></div>
          <button className="primary-button" type="submit" disabled={Boolean(loading)}><RefreshCw className={loading ? 'spin' : ''} size={17} />{loading ? 'Generando...' : 'Generar'}</button>
        </form>

        {error && <div className="sale-error" role="alert"><AlertTriangle size={19} /><span>{error}</span></div>}
        {loading === active && !report ? <ReportLoading /> : active === 'sales' ? <SalesReport report={report} /> : active === 'products' ? <ProductsReport report={report} /> : <CashReport report={report} />}
      </div>
    </AppShell>
  )
}

function SalesReport({ report }) {
  if (!report) return null
  const { metricas, ventas } = report
  return <div className="report-content"><div className="report-metrics"><ReportMetric icon={<ReceiptText size={20} />} label="Ventas confirmadas" value={metricas.cantidad} tone="blue" /><ReportMetric icon={<CircleDollarSign size={20} />} label="Total vendido" value={formatMoney(metricas.total)} tone="green" /><ReportMetric icon={<PackageOpen size={20} />} label="Costo total" value={formatMoney(metricas.costo)} tone="amber" /><ReportMetric icon={<TrendingUp size={20} />} label="Ganancia bruta" value={formatMoney(metricas.ganancia)} tone="teal" /></div><ReportTable title="Ventas del periodo" count={ventas.length} empty="No hay ventas confirmadas en las fechas seleccionadas."><table className="report-table report-table--sales"><thead><tr><th>Venta</th><th>Fecha</th><th>Cliente</th><th>Vendedor</th><th>Pago</th><th className="align-right">Total</th><th className="align-right">Costo</th><th className="align-right">Ganancia</th></tr></thead><tbody>{ventas.map((sale) => <tr key={sale.id}><td><strong>#{sale.id}</strong></td><td>{formatDate(sale.fecha)}</td><td>{sale.cliente}</td><td>{sale.usuario}</td><td><span className="payment-label">{formatLabel(sale.formaPago)}</span></td><td className="align-right money-cell">{formatMoney(sale.total)}</td><td className="align-right">{formatMoney(sale.costo)}</td><td className="align-right report-profit">{formatMoney(sale.ganancia)}</td></tr>)}</tbody></table></ReportTable></div>
}

function ProductsReport({ report }) {
  if (!report) return null
  const { metricas, productos } = report
  return <div className="report-content"><div className="report-metrics"><ReportMetric icon={<Trophy size={20} />} label="Productos vendidos" value={metricas.productos} tone="amber" /><ReportMetric icon={<PackageOpen size={20} />} label="Unidades" value={formatNumber(metricas.unidades)} tone="blue" /><ReportMetric icon={<CircleDollarSign size={20} />} label="Total facturado" value={formatMoney(metricas.facturado)} tone="green" /><ReportMetric icon={<TrendingUp size={20} />} label="Ganancia generada" value={formatMoney(metricas.ganancia)} tone="teal" /></div><ReportTable title="Ranking de productos" count={productos.length} empty="No hay productos vendidos en las fechas seleccionadas."><table className="report-table report-table--products"><thead><tr><th>Posicion</th><th>Codigo</th><th>Producto</th><th className="align-right">Cantidad</th><th className="align-right">Ventas</th><th className="align-right">Facturado</th><th className="align-right">Ganancia</th></tr></thead><tbody>{productos.map((product, index) => <tr key={product.productoId}><td><span className={index < 3 ? `ranking ranking--${index + 1}` : 'ranking'}>{index + 1}</span></td><td><span className="catalog-code">{product.codigo || 'S/C'}</span></td><td><strong>{product.nombre}</strong></td><td className="align-right">{formatNumber(product.cantidadVendida)}</td><td className="align-right">{product.cantidadVentas}</td><td className="align-right money-cell">{formatMoney(product.totalVendido)}</td><td className="align-right report-profit">{formatMoney(product.gananciaGenerada)}</td></tr>)}</tbody></table></ReportTable></div>
}

function CashReport({ report }) {
  if (!report) return null
  const { resumen, ventasPorMedio, pagosPorMedio, contadores, cajas, ventas, pagos } = report
  return <div className="report-content"><div className="report-metrics"><ReportMetric icon={<Banknote size={20} />} label="Ingresos por ventas" value={formatMoney(resumen.ingresosVentas)} tone="green" /><ReportMetric icon={<ReceiptText size={20} />} label="Pagos de clientes" value={formatMoney(resumen.pagosClientes)} tone="blue" /><ReportMetric icon={<CircleDollarSign size={20} />} label="Total ingresado" value={formatMoney(resumen.totalIngresado)} tone="teal" /><ReportMetric icon={<BarChart3 size={20} />} label="Ventas facturadas" value={formatMoney(resumen.ventasFacturadas)} tone="amber" /></div><div className="report-note"><AlertTriangle size={18} /><span>Las ventas a cuenta corriente se facturan, pero ingresan en caja cuando el cliente paga. Los pagos anulados no se incluyen.</span></div><div className="cash-counters"><span>Ventas <strong>{contadores.ventas}</strong></span><span>Pagos <strong>{contadores.pagos}</strong></span><span>Cajas <strong>{contadores.cajas}</strong></span><span>Abiertas <strong>{contadores.abiertas}</strong></span><span>Cerradas <strong>{contadores.cerradas}</strong></span></div><div className="cash-breakdowns"><Breakdown title="Ingresos por ventas" total={ventasPorMedio.totalIngresado} values={[['Efectivo', ventasPorMedio.efectivo], ['Transferencia', ventasPorMedio.transferencia], ['Tarjeta de debito', ventasPorMedio.debito], ['Tarjeta de credito', ventasPorMedio.credito], ['Cuenta corriente', ventasPorMedio.cuentaCorriente]]} /><Breakdown title="Pagos de clientes" total={pagosPorMedio.total} values={[['Efectivo', pagosPorMedio.efectivo], ['Transferencia', pagosPorMedio.transferencia], ['Tarjeta de debito', pagosPorMedio.debito], ['Tarjeta de credito', pagosPorMedio.credito], ['Otro', pagosPorMedio.otro]]} /></div><ReportTable title="Cajas del periodo" count={cajas.length} empty="No se abrieron cajas en las fechas seleccionadas."><table className="report-table report-table--cash"><thead><tr><th>Caja</th><th>Apertura</th><th>Cierre</th><th>Estado</th><th className="align-right">Ventas</th><th className="align-right">Pagos</th><th className="align-right">Total</th><th className="align-right">Esperado</th><th className="align-right">Contado</th><th className="align-right">Diferencia</th></tr></thead><tbody>{cajas.map((cash) => <tr key={cash.id}><td><strong>#{cash.id}</strong></td><td>{formatDate(cash.apertura)}</td><td>{formatDate(cash.cierre)}</td><td><span className={`cash-report-status cash-report-status--${cash.estado.toLowerCase()}`}>{formatLabel(cash.estado)}</span></td><td className="align-right">{formatMoney(cash.ventas)}</td><td className="align-right">{formatMoney(cash.pagosClientes)}</td><td className="align-right money-cell">{formatMoney(cash.total)}</td><td className="align-right">{formatMoney(cash.efectivoEsperado)}</td><td className="align-right">{cash.efectivoContado == null ? '-' : formatMoney(cash.efectivoContado)}</td><td className={`align-right ${Number(cash.diferencia) < 0 ? 'report-negative' : Number(cash.diferencia) > 0 ? 'report-profit' : ''}`}>{cash.diferencia == null ? '-' : formatMoney(cash.diferencia)}</td></tr>)}</tbody></table></ReportTable><div className="cash-report-tables"><ReportTable title="Ventas confirmadas" count={ventas.length} empty="No hay ventas confirmadas en el periodo."><table className="report-table"><thead><tr><th>Venta</th><th>Fecha</th><th>Cliente</th><th>Pago</th><th className="align-right">Facturado</th><th className="align-right">Ingreso</th></tr></thead><tbody>{ventas.map((sale) => <tr key={sale.id}><td><strong>#{sale.id}</strong></td><td>{formatDate(sale.fecha)}</td><td>{sale.cliente}</td><td>{formatLabel(sale.formaPago)}</td><td className="align-right">{formatMoney(sale.totalFacturado)}</td><td className="align-right money-cell">{formatMoney(sale.ingresoCaja)}</td></tr>)}</tbody></table></ReportTable><ReportTable title="Pagos de clientes" count={pagos.length} empty="No hay pagos vigentes en el periodo."><table className="report-table"><thead><tr><th>Pago</th><th>Fecha</th><th>Cliente</th><th>Medio</th><th>Observacion</th><th className="align-right">Monto</th></tr></thead><tbody>{pagos.map((payment) => <tr key={payment.id}><td><strong>#{payment.id}</strong></td><td>{formatDate(payment.fecha)}</td><td>{payment.cliente}</td><td>{formatLabel(payment.medioPago)}</td><td>{payment.observacion || '-'}</td><td className="align-right money-cell">{formatMoney(payment.monto)}</td></tr>)}</tbody></table></ReportTable></div></div>
}

function ReportMetric({ icon, label, value, tone }) {
  return <div className={`report-metric report-metric--${tone}`}><span>{icon}</span><div><small>{label}</small><strong title={String(value)}>{value}</strong></div></div>
}

function ReportTable({ title, count, empty, children }) {
  return <section className="report-panel"><header><div><h2>{title}</h2><span>{count} registros</span></div></header>{count ? <div className="table-scroll">{children}</div> : <div className="report-empty"><PackageOpen size={28} /><strong>Sin datos para mostrar</strong><span>{empty}</span></div>}</section>
}

function Breakdown({ title, total, values }) {
  return <section className="cash-breakdown"><header><h2>{title}</h2><strong>{formatMoney(total)}</strong></header><dl>{values.map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{formatMoney(value)}</dd></div>)}</dl></section>
}

function ReportLoading() {
  return <div className="report-loading" role="status"><div className="report-metrics">{Array.from({ length: 4 }, (_, index) => <div className="skeleton" key={index} />)}</div><div className="skeleton report-loading__table" /></div>
}

function formatInputDate(value) {
  if (!value) return '-'
  const [year, month, day] = value.split('-')
  return `${day}/${month}/${year}`
}
