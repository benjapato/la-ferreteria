import { useCallback, useEffect, useState } from 'react'
import {
  AlertTriangle, ArrowDownToLine, ArrowUpFromLine, ChevronLeft, ChevronRight,
  Filter, PackageSearch, RotateCcw, Search, SlidersHorizontal,
} from 'lucide-react'
import { apiRequest } from '../api/client'
import AppShell from '../components/AppShell'

const dateFormatter = new Intl.DateTimeFormat('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
const quantityFormatter = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 3 })
const initialFilters = { query: '', type: '', from: '', to: '' }

const typeLabels = {
  ENTRADA_COMPRA: 'Entrada por compra',
  SALIDA_VENTA: 'Salida por venta',
  ENTRADA_ANULACION_VENTA: 'Entrada por cancelacion',
  AJUSTE_ENTRADA: 'Ajuste de entrada',
  AJUSTE_SALIDA: 'Ajuste de salida',
}

export default function StockMovementsPage() {
  const [data, setData] = useState(null)
  const [filters, setFilters] = useState(initialFilters)
  const [applied, setApplied] = useState(initialFilters)
  const [advanced, setAdvanced] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadMovements = useCallback(async (page = 0, values = initialFilters) => {
    setLoading(true)
    setError('')
    const query = new URLSearchParams({ page: String(page) })
    Object.entries(values).forEach(([key, value]) => { if (value) query.set(key, value) })
    try {
      setData(await apiRequest(`/api/stock-movements?${query}`))
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadMovements(0, initialFilters) }, [loadMovements])

  function updateFilter(event) {
    setFilters((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  function applyFilters(event) {
    event.preventDefault()
    setApplied({ ...filters })
    loadMovements(0, filters)
  }

  function clearFilters() {
    setFilters(initialFilters)
    setApplied(initialFilters)
    setAdvanced(false)
    loadMovements(0, initialFilters)
  }

  const metrics = data?.metricas || { total: 0, entradas: 0, salidas: 0, ajustes: 0 }
  const hasFilters = Object.values(applied).some(Boolean)

  return (
    <AppShell>
      <div className="stock-movements-page">
        <div className="page-heading"><div><p className="eyebrow">Auditoria de inventario</p><h1>Movimientos de stock</h1><p>Historial de compras, ventas, cancelaciones y ajustes manuales.</p></div></div>

        <div className="stock-movement-metrics"><StockMetric icon={<PackageSearch size={20} />} label="Movimientos" value={metrics.total} tone="blue" /><StockMetric icon={<ArrowDownToLine size={20} />} label="Entradas" value={metrics.entradas} tone="green" /><StockMetric icon={<ArrowUpFromLine size={20} />} label="Salidas" value={metrics.salidas} tone="red" /><StockMetric icon={<SlidersHorizontal size={20} />} label="Ajustes manuales" value={metrics.ajustes} tone="amber" /></div>

        <form className="stock-movement-filters" onSubmit={applyFilters}>
          <div className="stock-movement-filters__main"><div className="filter-input"><Search size={17} /><input aria-label="Buscar producto" name="query" value={filters.query} onChange={updateFilter} placeholder="Codigo o nombre del producto" /></div><select aria-label="Tipo de movimiento" name="type" value={filters.type} onChange={updateFilter}><option value="">Todos los movimientos</option>{Object.entries(typeLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select><button className={advanced ? 'secondary-button filter-toggle filter-toggle--active' : 'secondary-button filter-toggle'} type="button" aria-label="Filtros por fecha" aria-expanded={advanced} onClick={() => setAdvanced((open) => !open)}><Filter size={17} /><span>Fechas</span></button><button className="primary-button" type="submit"><Search size={17} />Buscar</button><button className="icon-button reset-filters" type="button" title="Limpiar filtros" aria-label="Limpiar filtros" onClick={clearFilters}><RotateCcw size={18} /></button></div>
          {advanced && <div className="stock-movement-filters__advanced"><div className="form-field"><label htmlFor="stock-from">Fecha desde</label><input id="stock-from" name="from" type="date" value={filters.from} onChange={updateFilter} /></div><div className="form-field"><label htmlFor="stock-to">Fecha hasta</label><input id="stock-to" name="to" type="date" value={filters.to} onChange={updateFilter} /></div></div>}
        </form>

        {error && <div className="sale-error" role="alert"><AlertTriangle size={19} /><span>{error}</span></div>}

        <section className="stock-movements-panel"><header className="history-panel__header"><div><h2>Historial de movimientos</h2><span>{data?.totalElementos || 0} resultados{hasFilters ? ' con los filtros actuales' : ''}</span></div></header>{loading ? <div className="history-loading" role="status"><div className="skeleton" /><div className="skeleton" /><div className="skeleton" /></div> : data?.movimientos.length ? <div className="table-scroll"><table className="stock-movements-table"><thead><tr><th>Fecha</th><th>Producto</th><th>Tipo</th><th className="align-right">Cantidad</th><th className="align-right">Anterior</th><th className="align-right">Posterior</th><th>Responsable</th><th>Referencia</th><th>Observacion</th></tr></thead><tbody>{data.movimientos.map((movement) => { const entry = isEntry(movement.tipo); return <tr key={movement.id}><td>{dateFormatter.format(new Date(movement.fecha))}</td><td><div className="stock-movement-product"><span>{movement.codigo || 'S/C'}</span><div><strong>{movement.producto}</strong><small>{formatLabel(movement.unidad)}</small></div></div></td><td><span className={`stock-movement-type stock-movement-type--${entry ? 'entry' : 'exit'} ${movement.tipo.startsWith('AJUSTE') ? 'stock-movement-type--adjustment' : ''}`}>{typeLabels[movement.tipo]}</span></td><td className={`align-right stock-quantity stock-quantity--${entry ? 'entry' : 'exit'}`}>{entry ? '+' : '-'}{quantityFormatter.format(Number(movement.cantidad))}</td><td className="align-right">{quantityFormatter.format(Number(movement.stockAnterior))}</td><td className="align-right"><strong>{quantityFormatter.format(Number(movement.stockPosterior))}</strong></td><td>{movement.usuario}</td><td><Reference movement={movement} /></td><td className="stock-observation" title={movement.observacion || ''}>{movement.observacion || '-'}</td></tr> })}</tbody></table></div> : <div className="history-empty"><PackageSearch size={34} /><strong>{hasFilters ? 'No hay movimientos que coincidan' : 'Todavia no hay movimientos de stock'}</strong><span>{hasFilters ? 'Prueba modificando o limpiando los filtros.' : 'Las ventas, compras y ajustes apareceran aqui.'}</span></div>}{data && data.totalPaginas > 1 && <nav className="history-pagination" aria-label="Paginacion"><button className="icon-button" type="button" aria-label="Pagina anterior" disabled={data.primera || loading} onClick={() => loadMovements(data.pagina - 1, applied)}><ChevronLeft size={19} /></button><span>Pagina {data.pagina + 1} de {data.totalPaginas}</span><button className="icon-button" type="button" aria-label="Pagina siguiente" disabled={data.ultima || loading} onClick={() => loadMovements(data.pagina + 1, applied)}><ChevronRight size={19} /></button></nav>}</section>
      </div>
    </AppShell>
  )
}

function StockMetric({ icon, label, value, tone }) {
  return <div className={`stock-movement-metric stock-movement-metric--${tone}`}><span>{icon}</span><div><small>{label}</small><strong>{value}</strong></div></div>
}

function Reference({ movement }) {
  if (movement.referenciaTipo === 'COMPRA') return <span className="stock-reference stock-reference--purchase">Compra #{movement.referenciaId}</span>
  if (movement.referenciaTipo === 'VENTA') return <span className="stock-reference stock-reference--sale">Venta #{movement.referenciaId}</span>
  return <span className="stock-reference">Ajuste manual</span>
}

function isEntry(type) {
  return ['ENTRADA_COMPRA', 'ENTRADA_ANULACION_VENTA', 'AJUSTE_ENTRADA'].includes(type)
}

function formatLabel(value) {
  return String(value || '').toLowerCase().replaceAll('_', ' ').replace(/^./, (letter) => letter.toUpperCase())
}
