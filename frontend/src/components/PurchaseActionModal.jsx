import { AlertTriangle, Ban, PackageCheck } from 'lucide-react'
import Modal from './Modal'

const moneyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })

export default function PurchaseActionModal({ purchase, action, submitting, error, onClose, onConfirm }) {
  const confirming = action === 'confirm'

  return (
    <Modal
      title={confirming ? `Confirmar compra #${purchase.id}` : `Cancelar compra #${purchase.id}`}
      description={confirming ? 'La recepcion quedara aplicada de forma definitiva.' : 'La compra quedara cerrada sin recibir mercaderia.'}
      onClose={onClose}
      footer={(
        <>
          <button className="secondary-button" type="button" onClick={onClose} disabled={submitting}>Volver</button>
          <button className={confirming ? 'primary-button' : 'danger-button'} type="button" onClick={onConfirm} disabled={submitting}>
            {confirming ? <PackageCheck size={18} /> : <Ban size={18} />}
            {submitting ? 'Procesando...' : confirming ? 'Confirmar recepcion' : 'Cancelar compra'}
          </button>
        </>
      )}
    >
      <div className={confirming ? 'purchase-action-notice' : 'cancel-warning'}><AlertTriangle size={26} /><div><strong>{confirming ? 'El stock se actualizara ahora' : 'No se modificara el stock'}</strong><p>{confirming ? 'Cada producto sumara la cantidad comprada y tomara el costo unitario indicado como nuevo precio de compra.' : 'La compra pendiente se marcara como cancelada y no podra confirmarse despues.'}</p></div></div>
      <dl className="cancel-summary"><div><dt>Proveedor</dt><dd>{purchase.proveedor.nombre}</dd></div><div><dt>Total</dt><dd>{moneyFormatter.format(Number(purchase.total))}</dd></div></dl>
      {error && <p className="form-error" role="alert">{error}</p>}
    </Modal>
  )
}
