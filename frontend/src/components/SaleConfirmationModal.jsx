import { useRef } from 'react'
import { AlertTriangle, ArrowLeft, Check, UserRound } from 'lucide-react'
import Modal from './Modal'
import { lineSubtotal } from '../utils/quantities'

const money = (value) => new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(Number(value || 0))
const labels = { EFECTIVO: 'Efectivo', TRANSFERENCIA: 'Transferencia', TARJETA_DEBITO: 'Debito', TARJETA_CREDITO: 'Credito', CUENTA_CORRIENTE: 'Cuenta corriente' }
const balance = (value) => Number(value) > 0.005 ? `Debe ${money(value)}` : Number(value) < -0.005 ? `A favor ${money(-value)}` : 'Al dia'

export default function SaleConfirmationModal({ sale, submitting, error, onClose, onConfirm }) {
  const confirmRef = useRef(null)
  return (
    <Modal title="Confirmar venta" size="large" onClose={onClose} initialFocusRef={confirmRef}
      footer={<>
        <button className="secondary-button sale-review__action" type="button" onClick={onClose} disabled={submitting}><ArrowLeft size={18} />Volver a la venta</button>
        <button ref={confirmRef} className="primary-button sale-review__action" type="button" onClick={onConfirm} disabled={submitting}><Check size={18} />{submitting ? 'Registrando...' : 'Confirmar y registrar'}</button>
      </>}>
      <div className="sale-review">
        <div className="sale-review__client"><UserRound size={20} /><div><span>Cliente</span><strong>{sale.cliente}</strong></div></div>
        <div className="sale-review__receipt"><span>Comprobante</span><strong>{sale.formatoComprobante === 'a4' ? 'Hoja A4' : 'Ticket termico'}</strong></div>
        <div className="sale-review__products table-scroll">
          <table>
            <thead><tr><th>Producto</th><th className="align-right">Cantidad</th><th className="align-right">Precio</th><th className="align-right">Subtotal</th></tr></thead>
            <tbody>{sale.items.map((item) => <tr key={item.id}><td><strong>{item.nombre}</strong><small>{item.codigo}</small></td><td className="align-right" data-label="Cantidad">{item.cantidad}</td><td className="align-right" data-label="Precio">{money(item.precio)}</td><td className="align-right" data-label="Subtotal">{money(lineSubtotal(item.precio, item.cantidad))}</td></tr>)}</tbody>
          </table>
        </div>
        <dl className="sale-review__payments">{sale.pagos.map((payment) => <div key={payment.formaPago}><dt>{labels[payment.formaPago]}</dt><dd>{money(payment.monto)}</dd></div>)}</dl>
        {sale.saldoPosterior !== null && <dl className="sale-review__balance"><div><dt>Saldo anterior</dt><dd>{balance(sale.saldoAnterior)}</dd></div><div><dt>Saldo despues de la venta</dt><dd>{balance(sale.saldoPosterior)}</dd></div></dl>}
        {sale.body.observaciones && <p className="sale-review__observations">{sale.body.observaciones}</p>}
        <div className="sale-review__total"><span>Total de la venta</span><strong>{money(sale.total)}</strong></div>
        {error && <div className="sale-error" role="alert"><AlertTriangle size={19} /><span>{error}</span></div>}
      </div>
    </Modal>
  )
}
