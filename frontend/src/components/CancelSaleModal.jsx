import { AlertTriangle, Ban } from 'lucide-react'
import Modal from './Modal'

const moneyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })

export default function CancelSaleModal({ sale, submitting, error, onClose, onConfirm }) {
  return (
    <Modal
      title={`Cancelar venta #${sale.id}`}
      description="Esta operacion afecta stock, caja y cuenta corriente."
      onClose={onClose}
      footer={(
        <>
          <button className="secondary-button" type="button" onClick={onClose} disabled={submitting}>Conservar venta</button>
          <button className="danger-button" type="button" onClick={onConfirm} disabled={submitting}><Ban size={18} />{submitting ? 'Cancelando...' : 'Confirmar cancelacion'}</button>
        </>
      )}
    >
      <div className="cancel-warning"><AlertTriangle size={27} /><div><strong>La venta no se eliminara</strong><p>Quedara marcada como cancelada, los productos volveran al stock y se descontara el cobro de la caja.</p></div></div>
      <dl className="cancel-summary"><div><dt>Cliente</dt><dd>{sale.cliente}</dd></div><div><dt>Total</dt><dd>{moneyFormatter.format(Number(sale.total))}</dd></div></dl>
      {error && <p className="form-error" role="alert">{error}</p>}
    </Modal>
  )
}
