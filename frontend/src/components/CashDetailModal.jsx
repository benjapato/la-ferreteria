import { Banknote, CalendarDays, UserRound } from 'lucide-react'
import Modal from './Modal'

const moneyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', minimumFractionDigits: 2 })
const dateFormatter = new Intl.DateTimeFormat('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
const money = (value) => moneyFormatter.format(Number(value || 0))
const date = (value) => value ? dateFormatter.format(new Date(value)) : '-'

export default function CashDetailModal({ cash, onClose }) {
  return (
    <Modal title={`Detalle de caja #${cash.id}`} description="Arqueo, cobros y movimientos manuales registrados." size="large" onClose={onClose} footer={<button className="secondary-button" type="button" onClick={onClose}>Cerrar</button>}>
      <div className="cash-detail-status"><span className={`cash-box-status ${cash.arqueoPendiente ? 'cash-box-status--pending' : `cash-box-status--${cash.estado.toLowerCase()}`}`}>{cash.arqueoPendiente ? 'Arqueo pendiente' : cash.cierreAutomatico ? 'Cierre automatico' : cash.estado === 'ABIERTA' ? 'Abierta' : 'Cerrada'}</span><div><CalendarDays size={16} /><span>{date(cash.apertura)} a {date(cash.cierre)}</span></div><div><UserRound size={16} /><span>{cash.abiertaPor}{cash.cerradaPor ? ` / ${cash.cerradaPor}` : cash.cierreAutomatico ? ' / Sistema' : ''}</span></div></div>
      <div className="cash-detail-grid"><CashBreakdown title="Ventas" data={cash.ventas} rows={[['Efectivo', 'efectivo'], ['Transferencia', 'transferencia'], ['Tarjeta de debito', 'debito'], ['Tarjeta de credito', 'credito']]} /><CashBreakdown title="Pagos de clientes" data={cash.pagosClientes} rows={[['Efectivo', 'efectivo'], ['Transferencia', 'transferencia'], ['Tarjeta de debito', 'debito'], ['Tarjeta de credito', 'credito'], ['Otro', 'otro']]} /><CashBreakdown title="Arqueo de efectivo" data={cash.arqueo} rows={[['Monto inicial', 'montoInicial'], ['Ingresos manuales', 'ingresos'], ['Retiros', 'retiros'], ['Efectivo esperado', 'efectivoEsperado'], ['Efectivo contado', 'efectivoContado'], ['Diferencia', 'diferencia']]} /></div>
      <section className="cash-detail-total"><Banknote size={21} /><span>Total general</span><strong>{money(cash.totalGeneral)}</strong></section>
      <section className="cash-detail-movements"><h3>Ingresos y retiros manuales</h3>{cash.movimientos.length ? <div className="table-scroll"><table><thead><tr><th>Fecha</th><th>Tipo</th><th className="align-right">Monto</th><th>Motivo</th><th>Usuario</th></tr></thead><tbody>{cash.movimientos.map((movement) => <tr key={movement.id}><td>{date(movement.fecha)}</td><td><span className={`cash-movement-badge cash-movement-badge--${movement.tipo.toLowerCase()}`}>{movement.tipo === 'INGRESO' ? 'Ingreso' : 'Retiro'}</span></td><td className="align-right money-cell">{money(movement.monto)}</td><td>{movement.motivo}</td><td>{movement.usuario}</td></tr>)}</tbody></table></div> : <div className="compact-empty"><Banknote size={25} /><strong>Sin movimientos manuales</strong><span>Esta caja no tuvo ingresos ni retiros.</span></div>}</section>
    </Modal>
  )
}

function CashBreakdown({ title, data, rows }) {
  return <section className="cash-detail-breakdown"><h3>{title}</h3><dl>{rows.map(([label, key]) => <div key={key}><dt>{label}</dt><dd>{data[key] == null ? '-' : money(data[key])}</dd></div>)}</dl>{data.total != null && <footer><span>Total</span><strong>{money(data.total)}</strong></footer>}</section>
}
