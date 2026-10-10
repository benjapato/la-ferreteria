import { Ban, CalendarDays, PackageCheck, Truck, UserRound } from 'lucide-react'
import Modal from './Modal'

const moneyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })
const quantityFormatter = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 3 })
const dateFormatter = new Intl.DateTimeFormat('es-AR', {
  day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
})

export default function PurchaseDetailModal({ purchase, onClose, onConfirm, onCancel }) {
  const pending = purchase.estado === 'PENDIENTE'

  return (
    <Modal
      title={`Compra #${purchase.id}`}
      description={dateFormatter.format(new Date(purchase.fecha))}
      onClose={onClose}
      size="large"
      footer={(
        <>
          <button className="secondary-button" type="button" onClick={onClose}>Cerrar</button>
          {pending && <button className="danger-button" type="button" onClick={onCancel}><Ban size={18} />Cancelar</button>}
          {pending && <button className="primary-button" type="button" onClick={onConfirm}><PackageCheck size={18} />Confirmar recepcion</button>}
        </>
      )}
    >
      <div className={`purchase-state-notice purchase-state-notice--${purchase.estado.toLowerCase()}`}>
        {pending ? <CalendarDays size={23} /> : purchase.estado === 'CONFIRMADA' ? <PackageCheck size={23} /> : <Ban size={23} />}
        <div><strong>{pending ? 'Compra pendiente' : purchase.estado === 'CONFIRMADA' ? 'Mercaderia recibida' : 'Compra cancelada'}</strong><span>{pending ? 'El stock todavia no fue modificado.' : purchase.estado === 'CONFIRMADA' ? 'El stock y los costos ya fueron actualizados.' : 'Esta compra no modifico el stock.'}</span></div>
      </div>

      <div className="purchase-detail-meta">
        <div><span>Proveedor</span><strong><Truck size={14} />{purchase.proveedor.nombre}</strong><small>{purchase.proveedor.cuit || 'Sin CUIT'}</small></div>
        <div><span>Registrada por</span><strong><UserRound size={14} />{purchase.usuario}</strong></div>
        <div><span>Estado</span><strong className={`purchase-detail-status purchase-detail-status--${purchase.estado.toLowerCase()}`}>{formatLabel(purchase.estado)}</strong></div>
      </div>

      <section className="detail-products purchase-detail-products">
        <h3>Productos comprados</h3>
        <div className="table-scroll"><table><thead><tr><th>Codigo</th><th>Producto</th><th className="align-right">Cantidad</th><th className="align-right">Costo unitario</th><th className="align-right">Subtotal</th></tr></thead><tbody>{purchase.detalles.map((detail) => <tr key={detail.id || detail.productoId}><td><strong>{detail.codigo}</strong></td><td>{detail.producto}</td><td className="align-right">{quantityFormatter.format(Number(detail.cantidad))} {formatLabel(detail.unidadMedida)}</td><td className="align-right">{moneyFormatter.format(Number(detail.costoUnitario))}</td><td className="align-right money-cell">{moneyFormatter.format(Number(detail.subtotal))}</td></tr>)}</tbody></table></div>
      </section>

      <div className="purchase-detail-bottom"><div className="detail-observations"><span>Observaciones</span><p>{purchase.observaciones || 'Sin observaciones'}</p></div><div className="purchase-detail-total"><span>Total de la compra</span><strong>{moneyFormatter.format(Number(purchase.total))}</strong></div></div>
    </Modal>
  )
}

function formatLabel(value) {
  return String(value || '').toLowerCase().replaceAll('_', ' ').replace(/^./, (letter) => letter.toUpperCase())
}
