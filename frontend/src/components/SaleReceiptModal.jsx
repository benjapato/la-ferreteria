import { useEffect, useRef, useState } from 'react'
import { flushSync } from 'react-dom'
import { Printer, ShoppingCart, FileText, Settings } from 'lucide-react'
import Modal from './Modal'
import { printDocument } from '../utils/printDocument'
import { sendTicket } from '../utils/ticketPrinting'
import { useAuth } from '../auth/useAuth'
import PrinterSettingsModal from './PrinterSettingsModal'
import TicketPreview from './TicketPreview'

const moneyFormatter = new Intl.NumberFormat('es-AR', {
  style: 'currency',
  currency: 'ARS',
  minimumFractionDigits: 2,
})
const quantityFormatter = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 3 })
const dateFormatter = new Intl.DateTimeFormat('es-AR', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

export default function SaleReceiptModal({ sale, onClose, historical = false }) {
  const { user } = useAuth()
  const [format, setFormat] = useState(sale.formatoComprobante || 'a4')
  const [status, setStatus] = useState(null)
  const [printing, setPrinting] = useState(Boolean(sale.printPromise))
  const [configuring, setConfiguring] = useState(false)
  const printingRef = useRef(Boolean(sale.printPromise))
  useEffect(() => {
    let active = true
    if (sale.printPromise) sale.printPromise.then((result) => {
      if (active) { setStatus(result); setPrinting(false); printingRef.current = false }
    })
    return () => { active = false }
  }, [sale.printPromise])
  async function printTicket() {
    if (printingRef.current) return
    if ((status?.estado === 'ENVIADO' || status?.estado === 'REVISAR')
      && !window.confirm('Revise la cola de Windows y el papel antes de continuar. Se enviara otra copia del ticket.')) return
    printingRef.current = true
    setPrinting(true)
    setFormat('ticket')
    const result = await sendTicket(sale.id)
    setStatus(result)
    setPrinting(false)
    printingRef.current = false
  }
  function printA4() {
    flushSync(() => setFormat('a4'))
    printDocument('printing-receipt')
  }
  const canceled = sale.estado === 'CANCELADA'
  const customer = String(sale.cliente || '').trim() || 'Consumidor final'
  const receiptNumber = String(sale.id).padStart(8, '0')
  const payments = sale.pagos || []
  if (configuring) return <PrinterSettingsModal onClose={() => setConfiguring(false)} />

  return (
    <Modal
      title={`Comprobante de venta #${sale.id}`}
      onClose={onClose}
      size="large"
      footer={(
        <>
          <button className="secondary-button" type="button" disabled={printing} onClick={printTicket}><Printer size={18} />{printing ? 'Enviando ticket...' : status?.estado === 'ENVIADO' || status?.estado === 'REVISAR' ? 'Reimprimir ticket' : 'Imprimir ticket'}</button>
          <button className="secondary-button" type="button" onClick={printA4}><FileText size={18} />Factura (A4)</button>
          <button className="primary-button" type="button" onClick={onClose}>{historical ? 'Cerrar' : <><ShoppingCart size={18} />Nueva venta</>}</button>
        </>
      )}
    >
      <div className="receipt-toolbar">
        <div className="receipt-modes" role="group" aria-label="Vista del comprobante">
          <button type="button" aria-pressed={format === 'ticket'} onClick={() => setFormat('ticket')}><Printer size={17} />Ticket 58 mm</button>
          <button type="button" aria-pressed={format === 'a4'} onClick={() => setFormat('a4')}><FileText size={17} />Hoja A4</button>
        </div>
        {user.rol === 'ADMINISTRADOR' && <button type="button" className="printer-config-button" disabled={printing} title="Configurar impresora de tickets" aria-label="Configurar impresora de tickets" onClick={() => setConfiguring(true)}><Settings size={18} /><span>Configurar impresora</span></button>}
      </div>
      {printing && <div className="ticket-notice" role="status">Venta registrada. Enviando ticket...</div>}
      {status && <div className={'ticket-notice' + (status.estado === 'ENVIADO' ? ' ticket-notice--success' : '')} role="status">{status.mensaje}</div>}
      {format === 'ticket' && <TicketPreview sale={sale} />}
      <div className={format === 'ticket' ? 'a4-receipt a4-receipt--hidden' : 'a4-receipt'}>
      <article className={`sale-receipt ${canceled ? 'sale-receipt--canceled' : ''}`} aria-label={`Comprobante de venta ${sale.id}`}>
        <header className="sale-receipt__header">
          <div className="sale-receipt__brand">
            <span className="sale-receipt__mark">LF</span>
            <div>
              <strong>LaFerreteria</strong>
              <span>Ferreteria y materiales</span>
            </div>
          </div>
          <div className="sale-receipt__identity">
            <span>Comprobante de venta</span>
            <strong>N. {receiptNumber}</strong>
            <small>Documento no fiscal</small>
          </div>
        </header>

        <div className={`sale-receipt__status ${canceled ? 'sale-receipt__status--canceled' : ''}`}>
          <span>{canceled ? 'Venta cancelada' : 'Venta confirmada'}</span>
          <strong>{moneyFormatter.format(Number(sale.total))}</strong>
        </div>

        <section className="sale-receipt__information" aria-label="Datos de la venta">
          <ReceiptDatum label="Fecha y hora" value={formatDate(sale.fecha)} />
          <ReceiptDatum label="Cliente" value={customer} emphasis />
          <ReceiptDatum label="DNI / CUIT" value={sale.documentoCliente || 'No informado'} />
          <ReceiptDatum label="Domicilio" value={sale.direccionCliente || 'No informado'} />
          <ReceiptDatum label="Vendedor" value={sale.vendedor || 'Sin registrar'} />
          <ReceiptDatum label="Caja" value={sale.cajaId ? `N. ${sale.cajaId}` : 'Sin asociar'} />
          <ReceiptDatum label="Forma de pago" value={formatLabel(sale.formaPago)} />
          <ReceiptDatum label="Estado" value={formatLabel(sale.estado || 'CONFIRMADA')} />
        </section>

        <section className="sale-receipt__products">
          <div className="sale-receipt__section-heading">
            <h3>Detalle de productos</h3>
            <span>{sale.detalles.length} {sale.detalles.length === 1 ? 'renglon' : 'renglones'}</span>
          </div>
          <div className="sale-receipt__table-wrap">
            <table>
              <thead><tr><th>Codigo</th><th>Producto</th><th className="align-right">Cantidad</th><th className="align-right">Precio unitario</th><th className="align-right">Subtotal</th></tr></thead>
              <tbody>
                {sale.detalles.map((detail) => (
                  <tr key={detail.productoId}>
                    <td><span className="sale-receipt__code">{detail.codigo || 'S/C'}</span></td>
                    <td><strong>{detail.nombre}</strong></td>
                    <td className="align-right">{quantityFormatter.format(Number(detail.cantidad))}</td>
                    <td className="align-right">{moneyFormatter.format(Number(detail.precioUnitario))}</td>
                    <td className="align-right sale-receipt__money">{moneyFormatter.format(Number(detail.subtotal))}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        <div className="sale-receipt__closing">
          <div className="sale-receipt__notes-payments">
            <section className="sale-receipt__payments">
              <h3>Pago</h3>
              {payments.length > 0 ? payments.map((payment) => (
                <div key={payment.formaPago}>
                  <span>{payment.formaPago === 'CUENTA_CORRIENTE' && Number(payment.monto) < 0 ? 'Saldo a favor' : formatLabel(payment.formaPago)}</span>
                  <strong className={Number(payment.monto) < 0 ? 'sale-receipt__credit' : ''}>{formatPaymentAmount(payment.monto)}</strong>
                </div>
              )) : (
                <div><span>{formatLabel(sale.formaPago)}</span><strong>{moneyFormatter.format(Number(sale.total))}</strong></div>
              )}
            </section>

            {sale.observaciones && (
              <section className="sale-receipt__observations">
                <h3>Observaciones</h3>
                <p>{sale.observaciones}</p>
              </section>
            )}
          </div>

          <dl className="sale-receipt__totals">
            <div><dt>Subtotal</dt><dd>{moneyFormatter.format(Number(sale.total))}</dd></div>
            <div className="sale-receipt__grand-total"><dt>Total</dt><dd>{moneyFormatter.format(Number(sale.total))}</dd></div>
          </dl>
        </div>

        <footer className="sale-receipt__footer">
          <strong>Gracias por su compra.</strong>
          <span>Conserve este comprobante para cualquier consulta o cambio.</span>
          <small>Este comprobante es de uso interno y no reemplaza una factura fiscal.</small>
        </footer>
      </article>
      </div>
    </Modal>
  )
}

function ReceiptDatum({ label, value, emphasis = false }) {
  return <div><span>{label}</span><strong className={emphasis ? 'sale-receipt__customer' : ''}>{value}</strong></div>
}

function formatLabel(value) {
  return String(value || '').toLowerCase().replaceAll('_', ' ').replace(/^./, (letter) => letter.toUpperCase())
}

function formatDate(value) {
  if (!value) return 'Sin registrar'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? 'Sin registrar' : dateFormatter.format(date)
}

function formatPaymentAmount(value) {
  const amount = Number(value || 0)
  return amount < 0
    ? `-${moneyFormatter.format(Math.abs(amount))}`
    : moneyFormatter.format(amount)
}
