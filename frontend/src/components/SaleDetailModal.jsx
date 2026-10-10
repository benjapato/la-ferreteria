import { Ban, FileText, UserRound } from 'lucide-react'
import Modal from './Modal'

const moneyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })
const dateFormatter = new Intl.DateTimeFormat('es-AR', {
  day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
})

export default function SaleDetailModal({ sale, isAdmin, onClose, onReceipt, onCancel }) {
  const canceled = sale.estado === 'CANCELADA'

  return (
    <Modal
      title={`Detalle de venta #${sale.id}`}
      description={dateFormatter.format(new Date(sale.fecha))}
      onClose={onClose}
      size="large"
      footer={(
        <>
          <button className="secondary-button" type="button" onClick={onClose}>Cerrar</button>
          <button className="secondary-button" type="button" onClick={onReceipt}><FileText size={18} />Comprobante</button>
          {isAdmin && !canceled && <button className="danger-button" type="button" onClick={onCancel}><Ban size={18} />Cancelar venta</button>}
        </>
      )}
    >
      {canceled && <div className="canceled-notice"><Ban size={20} /><div><strong>Venta cancelada</strong><span>El stock y los importes de caja ya fueron revertidos.</span></div></div>}

      <div className="sale-detail-meta">
        <div><span>Cliente</span><strong>{sale.cliente}</strong>{sale.documentoCliente && <small>{sale.documentoCliente}</small>}</div>
        <div><span>Forma de pago</span><strong>{formatLabel(sale.formaPago)}</strong></div>
        <div><span>Estado</span><strong className={canceled ? 'detail-status detail-status--canceled' : 'detail-status'}>{formatLabel(sale.estado)}</strong></div>
        <div><span>Vendedor</span><strong><UserRound size={14} />{sale.vendedor}</strong></div>
        <div><span>Caja</span><strong>{sale.cajaId ? `#${sale.cajaId}` : 'Sin asociar'}</strong></div>
      </div>

      {sale.pagos.length > 1 && (
        <section className="detail-payments"><h3>Distribucion del pago</h3><div>{sale.pagos.map((payment) => <span key={payment.formaPago}><small>{formatLabel(payment.formaPago)}</small><strong>{moneyFormatter.format(Number(payment.monto))}</strong></span>)}</div></section>
      )}

      <section className="detail-products">
        <h3>Productos vendidos</h3>
        <div className="table-scroll">
          <table>
            <thead><tr><th>Codigo</th><th>Producto</th><th>Cantidad</th><th className="align-right">Precio</th><th className="align-right">Subtotal</th><th className="align-right">Ganancia</th></tr></thead>
            <tbody>{sale.detalles.map((detail) => <tr key={detail.productoId}><td><strong>{detail.codigo}</strong></td><td>{detail.nombre}</td><td>{detail.cantidad}</td><td className="align-right">{moneyFormatter.format(Number(detail.precioUnitario))}</td><td className="align-right money-cell">{moneyFormatter.format(Number(detail.subtotal))}</td><td className="align-right detail-profit">{moneyFormatter.format(Number(detail.ganancia))}</td></tr>)}</tbody>
          </table>
        </div>
      </section>

      <div className="detail-bottom">
        <div className="detail-observations"><span>Observaciones</span><p>{sale.observaciones || 'Sin observaciones'}</p></div>
        <dl className="detail-totals"><div><dt>Costo</dt><dd>{moneyFormatter.format(Number(sale.costoTotal))}</dd></div><div><dt>Ganancia</dt><dd className="detail-profit">{moneyFormatter.format(Number(sale.gananciaTotal))}</dd></div><div><dt>Total</dt><dd>{moneyFormatter.format(Number(sale.total))}</dd></div></dl>
      </div>
    </Modal>
  )
}

function formatLabel(value) {
  return String(value || '').toLowerCase().replaceAll('_', ' ').replace(/^./, (letter) => letter.toUpperCase())
}
