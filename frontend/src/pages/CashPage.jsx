import { useCallback, useEffect, useState } from 'react'
import {
  AlertTriangle, ArrowDownToLine, ArrowUpFromLine, Banknote, ChevronLeft,
  ChevronRight, CircleDollarSign, Eye, History, LockKeyhole, RefreshCw, WalletCards,
} from 'lucide-react'
import { apiRequest } from '../api/client'
import AppShell from '../components/AppShell'
import CashCloseModal from '../components/CashCloseModal'
import CashDetailModal from '../components/CashDetailModal'
import CashMovementModal from '../components/CashMovementModal'
import CashOpeningModal from '../components/CashOpeningModal'

const moneyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', minimumFractionDigits: 2 })
const dateFormatter = new Intl.DateTimeFormat('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
const money = (value) => moneyFormatter.format(Number(value || 0))
const date = (value) => value ? dateFormatter.format(new Date(value)) : '-'

export default function CashPage() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [modal, setModal] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)

  const loadCash = useCallback(async (page = 0) => {
    setLoading(true)
    setError('')
    try {
      setData(await apiRequest(`/api/cash?page=${page}`))
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadCash() }, [loadCash])

  async function refreshAfterAction() {
    setModal(null)
    await loadCash(0)
  }

  async function openDetail(id) {
    setDetailLoading(true)
    setError('')
    try {
      setModal({ type: 'detail', cash: await apiRequest(`/api/cash/${id}`) })
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setDetailLoading(false)
    }
  }

  const cash = data?.actual
  const pendingReconciliation = data?.pendienteArqueo

  return (
    <AppShell onCashChanged={() => loadCash(data?.pagina || 0)} cashStatusKey={`${cash?.id || ''}:${pendingReconciliation?.id || ''}`}>
      <div className="cash-page">
        <div className="page-heading cash-page-heading">
          <div><p className="eyebrow">Operacion diaria</p><h1>Caja</h1><p>Controla cobros, efectivo disponible y cierres de turno.</p></div>
          <button className="secondary-button" type="button" aria-label="Actualizar caja" title="Actualizar caja" onClick={() => loadCash(data?.pagina || 0)} disabled={loading}><RefreshCw className={loading ? 'spin' : ''} size={18} /><span>Actualizar</span></button>
        </div>

        {error && <div className="sale-error" role="alert"><AlertTriangle size={19} /><span>{error}</span></div>}

        {loading && !data ? <CashLoading /> : cash ? <CurrentCash cash={cash} onMovement={(type) => setModal({ type: 'movement', movementType: type })} onClose={() => setModal({ type: 'close' })} /> : <ClosedCash pending={pendingReconciliation} onOpen={() => setModal({ type: 'open' })} onReconcile={() => setModal({ type: 'reconcile' })} />}

        <section className="cash-history-panel">
          <header className="history-panel__header"><div><h2>Historial de cajas</h2><span>{data?.totalElementos || 0} cajas registradas</span></div></header>
          {loading ? <div className="history-loading" role="status"><div className="skeleton" /><div className="skeleton" /><div className="skeleton" /></div> : data?.cajas.length ? <div className="table-scroll"><table className="cash-history-table"><thead><tr><th>Caja</th><th>Apertura</th><th>Cierre</th><th>Responsables</th><th>Estado</th><th className="align-right">Ventas</th><th className="align-right">Pagos</th><th className="align-right">Total</th><th className="align-right">Esperado</th><th className="align-right">Contado</th><th className="align-right">Diferencia</th><th className="align-right">Detalle</th></tr></thead><tbody>{data.cajas.map((item) => <tr key={item.id}><td><strong>#{item.id}</strong></td><td>{date(item.apertura)}</td><td>{date(item.cierre)}</td><td><div className="cash-users"><strong>{item.abiertaPor}</strong><span>{item.arqueoPendiente ? 'Cierre automatico' : item.cierreAutomatico ? `Regularizado por ${item.cerradaPor || 'administrador'}` : item.cerradaPor || 'Sin cierre'}</span></div></td><td><span className={`cash-box-status ${item.arqueoPendiente ? 'cash-box-status--pending' : `cash-box-status--${item.estado.toLowerCase()}`}`}>{item.arqueoPendiente ? 'Arqueo pendiente' : item.cierreAutomatico ? 'Cierre automatico' : item.estado === 'ABIERTA' ? 'Abierta' : 'Cerrada'}</span></td><td className="align-right">{money(item.totalVentas)}</td><td className="align-right">{money(item.pagosClientes)}</td><td className="align-right money-cell">{money(item.totalGeneral)}</td><td className="align-right">{money(item.efectivoEsperado)}</td><td className="align-right">{item.efectivoContado == null ? '-' : money(item.efectivoContado)}</td><td className={`align-right ${Number(item.diferencia) < 0 ? 'report-negative' : Number(item.diferencia) > 0 ? 'report-profit' : ''}`}>{item.diferencia == null ? '-' : money(item.diferencia)}</td><td><div className="cash-history-action"><button className="icon-button" type="button" title="Ver detalle" aria-label={`Ver caja ${item.id}`} disabled={detailLoading} onClick={() => openDetail(item.id)}><Eye size={17} /></button></div></td></tr>)}</tbody></table></div> : <div className="history-empty"><History size={34} /><strong>Todavia no hay cajas registradas</strong><span>La primera apertura aparecera en este historial.</span></div>}
          {data && data.totalPaginas > 1 && <nav className="history-pagination" aria-label="Paginacion"><button className="icon-button" type="button" aria-label="Pagina anterior" disabled={data.primera || loading} onClick={() => loadCash(data.pagina - 1)}><ChevronLeft size={19} /></button><span>Pagina {data.pagina + 1} de {data.totalPaginas}</span><button className="icon-button" type="button" aria-label="Pagina siguiente" disabled={data.ultima || loading} onClick={() => loadCash(data.pagina + 1)}><ChevronRight size={19} /></button></nav>}
        </section>
      </div>

      {modal?.type === 'open' && <CashOpeningModal endpoint="/api/cash/open" onClose={() => setModal(null)} onOpened={refreshAfterAction} />}
      {modal?.type === 'movement' && <CashMovementModal initialType={modal.movementType} expectedCash={cash?.arqueo.efectivoEsperado} onClose={() => setModal(null)} onSaved={refreshAfterAction} />}
      {modal?.type === 'close' && <CashCloseModal cash={cash} onClose={() => setModal(null)} onClosed={refreshAfterAction} />}
      {modal?.type === 'reconcile' && pendingReconciliation && <CashCloseModal cash={pendingReconciliation} mode="reconcile" onClose={() => setModal(null)} onClosed={refreshAfterAction} />}
      {modal?.type === 'detail' && <CashDetailModal cash={modal.cash} onClose={() => setModal(null)} />}
    </AppShell>
  )
}

function CurrentCash({ cash, onMovement, onClose }) {
  return <section className="current-cash"><header className="current-cash__header"><div><span className="cash-live-dot" /><div><strong>Caja #{cash.id} abierta</strong><small>Desde {date(cash.apertura)} por {cash.abiertaPor}</small></div></div><div className="current-cash__actions"><button className="secondary-button cash-action--income" type="button" onClick={() => onMovement('INGRESO')}><ArrowDownToLine size={17} /><span>Ingreso</span></button><button className="secondary-button cash-action--withdrawal" type="button" onClick={() => onMovement('RETIRO')}><ArrowUpFromLine size={17} /><span>Retiro</span></button><button className="danger-button" type="button" onClick={onClose}><LockKeyhole size={17} /><span>Cerrar caja</span></button></div></header><div className="cash-main-metrics"><CashMetric icon={<Banknote size={20} />} label="Efectivo esperado" value={money(cash.arqueo.efectivoEsperado)} tone="green" /><CashMetric icon={<CircleDollarSign size={20} />} label="Ventas cobradas" value={money(cash.ventas.total)} tone="blue" /><CashMetric icon={<WalletCards size={20} />} label="Pagos de clientes" value={money(cash.pagosClientes.total)} tone="teal" /><CashMetric icon={<Banknote size={20} />} label="Total general" value={money(cash.totalGeneral)} tone="amber" /></div><div className="cash-flow-strip"><FlowItem label="Efectivo inicial" value={cash.arqueo.montoInicial} /><FlowItem label="Ventas en efectivo" value={cash.ventas.efectivo} /><FlowItem label="Pagos en efectivo" value={cash.pagosClientes.efectivo} /><FlowItem label="Ingresos manuales" value={cash.arqueo.ingresos} positive /><FlowItem label="Retiros" value={cash.arqueo.retiros} negative /></div><section className="current-cash-movements"><header><div><h2>Movimientos manuales</h2><span>{cash.movimientos.length} registrados en la caja actual</span></div></header>{cash.movimientos.length ? <div className="table-scroll"><table><thead><tr><th>Fecha</th><th>Tipo</th><th className="align-right">Monto</th><th>Motivo</th><th>Usuario</th></tr></thead><tbody>{cash.movimientos.map((movement) => <tr key={movement.id}><td>{date(movement.fecha)}</td><td><span className={`cash-movement-badge cash-movement-badge--${movement.tipo.toLowerCase()}`}>{movement.tipo === 'INGRESO' ? 'Ingreso' : 'Retiro'}</span></td><td className="align-right money-cell">{money(movement.monto)}</td><td>{movement.motivo}</td><td>{movement.usuario}</td></tr>)}</tbody></table></div> : <div className="compact-empty"><Banknote size={25} /><strong>Sin ingresos ni retiros manuales</strong><span>Los movimientos de efectivo apareceran aqui.</span></div>}</section></section>
}

function ClosedCash({ pending, onOpen, onReconcile }) {
  if (pending) {
    return <section className="closed-cash closed-cash--pending"><span><AlertTriangle size={26} /></span><div><h2>Caja #{pending.id} con arqueo pendiente</h2><p>Se cerro automaticamente el {date(pending.cierre)}. Registra el efectivo contado para calcular la diferencia.</p></div><button className="primary-button" type="button" onClick={onReconcile}><Banknote size={18} />Completar arqueo</button></section>
  }
  return <section className="closed-cash"><span><LockKeyhole size={26} /></span><div><h2>No hay una caja abierta</h2><p>Abre una nueva caja indicando el efectivo inicial para comenzar a operar.</p></div><button className="primary-button" type="button" onClick={onOpen}><Banknote size={18} />Abrir caja</button></section>
}

function CashMetric({ icon, label, value, tone }) {
  return <div className={`cash-main-metric cash-main-metric--${tone}`}><span>{icon}</span><div><small>{label}</small><strong title={value}>{value}</strong></div></div>
}

function FlowItem({ label, value, positive, negative }) {
  return <div className={positive ? 'cash-flow-item cash-flow-item--positive' : negative ? 'cash-flow-item cash-flow-item--negative' : 'cash-flow-item'}><span>{label}</span><strong>{money(value)}</strong></div>
}

function CashLoading() {
  return <div className="cash-loading" role="status"><div className="skeleton" /><div className="cash-main-metrics">{Array.from({ length: 4 }, (_, index) => <div className="skeleton" key={index} />)}</div><div className="skeleton" /></div>
}
