import { Ban, Undo2 } from 'lucide-react'
import { useState } from 'react'
import Modal from './Modal'

const moneyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })

export default function CancelPaymentModal({ payment, submitting, error, onClose, onConfirm }) {
  const [reason, setReason] = useState('')

  return (
    <Modal
      title={`Anular cobro #${payment.id}`}
      description="Esta accion queda registrada en la cuenta corriente."
      onClose={onClose}
      footer={(
        <>
          <button className="secondary-button" type="button" onClick={onClose} disabled={submitting}>Volver</button>
          <button className="danger-button" type="button" onClick={() => onConfirm(reason)} disabled={submitting}><Ban size={18} />{submitting ? 'Anulando...' : 'Anular cobro'}</button>
        </>
      )}
    >
      <div className="cancel-warning"><Undo2 size={24} /><div><strong>Se devolveran {moneyFormatter.format(Number(payment.monto))} al saldo del cliente</strong><p>El importe tambien se descontara de la caja donde se registro. La caja original debe continuar abierta.</p></div></div>
      <div className="form-field customer-cancel-reason"><label htmlFor="payment-cancel-reason">Motivo</label><textarea id="payment-cancel-reason" rows="3" maxLength="300" placeholder="Opcional" value={reason} onChange={(event) => setReason(event.target.value)} /></div>
      {error && <p className="form-error" role="alert">{error}</p>}
    </Modal>
  )
}
