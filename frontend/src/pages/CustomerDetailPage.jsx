import {
  AlertTriangle, ArrowLeft, Ban, ChevronLeft, ChevronRight, Edit3, Eye,
  FileText, HandCoins, History, Mail, MapPin, Phone, Power, PowerOff,
  ReceiptText, UserRound, WalletCards, X,
} from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { apiRequest } from '../api/client'
import { useAuth } from '../auth/useAuth'
import AppShell from '../components/AppShell'
import CancelPaymentModal from '../components/CancelPaymentModal'
import CancelSaleModal from '../components/CancelSaleModal'
import CustomerFormModal from '../components/CustomerFormModal'
import CustomerPaymentModal from '../components/CustomerPaymentModal'
import SaleDetailModal from '../components/SaleDetailModal'
import SaleReceiptModal from '../components/SaleReceiptModal'
import StatusChangeModal from '../components/StatusChangeModal'

const moneyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })
const dateFormatter = new Intl.DateTimeFormat('es-AR', {
  day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
})

export default function CustomerDetailPage() {
  const { id } = useParams()
  const { user } = useAuth()
  const isAdmin = user.rol === 'ADMINISTRADOR'
  const [detail, setDetail] = useState(null)
  const [movements, setMovements] = useState(null)
  const [sales, setSales] = useState(null)
  const [activeTab, setActiveTab] = useState('account')
  const [loading, setLoading] = useState(true)
  const [sectionLoading, setSectionLoading] = useState(false)
  const [actionLoading, setActionLoading] = useState('')
  const [modal, setModal] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [modalError, setModalError] = useState('')
  const [error, setError] = useState('')

  const loadCustomer = useCallback(async () => {
    const data = await apiRequest(`/api/customers/${id}`)
    setDetail(data)
    return data
  }, [id])

  const loadMovements = useCallback(async (page = 0, showLoading = true) => {
    if (showLoading) setSectionLoading(true)
    try {
      setMovements(await apiRequest(`/api/customers/${id}/movements?page=${page}`))
    } finally {
      if (showLoading) setSectionLoading(false)
    }
  }, [id])

  const loadSales = useCallback(async (page = 0, showLoading = true) => {
    if (showLoading) setSectionLoading(true)
    try {
      setSales(await apiRequest(`/api/customers/${id}/sales?page=${page}`))
    } finally {
      if (showLoading) setSectionLoading(false)
    }
  }, [id])

  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')
    Promise.all([loadCustomer(), loadMovements(0, false), loadSales(0, false)])
      .catch((requestError) => active && setError(requestError.message))
      .finally(() => active && setLoading(false))
    return () => { active = false }
  }, [loadCustomer, loadMovements, loadSales])

  async function refreshAccount() {
    setModal(null)
    setModalError('')
    setError('')
    try {
      await Promise.all([loadCustomer(), loadMovements(0, false)])
    } catch (requestError) {
      setError(requestError.message)
    }
  }

  async function confirmStatusChange() {
    setSubmitting(true)
    setModalError('')
    try {
      await apiRequest(`/api/customers/${id}/status`, { method: 'POST' })
      await loadCustomer()
      setModal(null)
    } catch (requestError) {
      setModalError(requestError.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function confirmPaymentCancellation(reason) {
    setSubmitting(true)
    setModalError('')
    try {
      await apiRequest(`/api/customers/${id}/payments/${modal.payment.id}/cancel`, { method: 'POST', body: { motivo: reason } })
      await refreshAccount()
    } catch (requestError) {
      setModalError(requestError.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function openSale(saleId, target = 'detail') {
    setActionLoading(`${target}-${saleId}`)
    setError('')
    try {
      const sale = await apiRequest(`/api/sales/${saleId}`)
      setModal({ type: target, sale })
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setActionLoading('')
    }
  }

  async function confirmSaleCancellation() {
    setSubmitting(true)
    setModalError('')
    try {
      await apiRequest(`/api/sales/${modal.sale.id}/cancel`, { method: 'POST' })
      setModal(null)
      await Promise.all([loadCustomer(), loadMovements(0, false), loadSales(sales?.pagina || 0, false)])
    } catch (requestError) {
      setModalError(requestError.message)
    } finally {
      setSubmitting(false)
    }
  }

  function changeTab(tab) {
    setActiveTab(tab)
    setError('')
  }

  const customer = detail?.cliente

  return (
    <AppShell>
      <div className="customer-detail-page">
        <Link className="back-link" to="/clientes"><ArrowLeft size={16} />Clientes</Link>

        {error && <div className="sale-error" role="alert"><AlertTriangle size={19} /><span>{error}</span><button className="icon-button" type="button" aria-label="Cerrar error" onClick={() => setError('')}><X size={18} /></button></div>}

        {loading ? <CustomerDetailLoading /> : customer ? (
          <>
            <div className="customer-detail-heading">
              <div><p className="eyebrow">Ficha del cliente</p><h1>{customer.nombre}</h1><div className="customer-detail-state"><span className={customer.activo ? 'record-status' : 'record-status record-status--inactive'}>{customer.activo ? 'Activo' : 'Inactivo'}</span>{customer.documento && <span>{customer.documento}</span>}</div></div>
              <div className="customer-heading-actions">
                <button className="secondary-button" type="button" onClick={() => setModal({ type: 'edit' })}><Edit3 size={17} /><span>Editar</span></button>
                <button className="secondary-button" type="button" onClick={() => setModal({ type: 'status' })}>{customer.activo ? <PowerOff size={17} /> : <Power size={17} />}<span>{customer.activo ? 'Desactivar' : 'Activar'}</span></button>
                <button className="primary-button" type="button" disabled={!detail.cajaAbierta || !customer.activo} title={!detail.cajaAbierta ? 'La caja esta cerrada' : !customer.activo ? 'El cliente esta inactivo' : 'Registrar cobro'} onClick={() => setModal({ type: 'payment' })}><HandCoins size={18} /><span>Registrar cobro</span></button>
              </div>
            </div>

            <section className="customer-overview">
              <div className="customer-contact-grid">
                <ContactItem icon={<Phone size={17} />} label="Telefono" value={customer.telefono || 'Sin telefono'} />
                <ContactItem icon={<Mail size={17} />} label="Correo" value={customer.email || 'Sin correo'} />
                <ContactItem icon={<MapPin size={17} />} label="Direccion" value={customer.direccion || 'Sin direccion'} />
              </div>
              <div className={`customer-balance-summary customer-balance-summary--${customer.estadoSaldo.toLowerCase()}`}><WalletCards size={23} /><div><span>{balanceLabel(customer.estadoSaldo)}</span><strong>{moneyFormatter.format(Number(customer.saldo))}</strong></div></div>
            </section>

            <div className="customer-tabs" role="tablist" aria-label="Informacion del cliente">
              <button className={activeTab === 'account' ? 'customer-tab customer-tab--active' : 'customer-tab'} type="button" role="tab" aria-selected={activeTab === 'account'} onClick={() => changeTab('account')}><WalletCards size={18} />Cuenta corriente<span>{movements?.totalElementos || 0}</span></button>
              <button className={activeTab === 'sales' ? 'customer-tab customer-tab--active' : 'customer-tab'} type="button" role="tab" aria-selected={activeTab === 'sales'} onClick={() => changeTab('sales')}><History size={18} />Ventas<span>{sales?.totalElementos || 0}</span></button>
            </div>

            {activeTab === 'account' ? (
              <AccountPanel data={movements} loading={sectionLoading} isAdmin={isAdmin} actionLoading={actionLoading} onPageChange={(page) => loadMovements(page)} onOpenSale={openSale} onCancelPayment={(payment) => { setModalError(''); setModal({ type: 'cancel-payment', payment }) }} />
            ) : (
              <SalesPanel data={sales} loading={sectionLoading} actionLoading={actionLoading} onPageChange={(page) => loadSales(page)} onOpenSale={openSale} />
            )}
          </>
        ) : !error && <div className="catalog-empty"><UserRound size={34} /><strong>No se encontro el cliente</strong><Link className="primary-button" to="/clientes">Volver a clientes</Link></div>}
      </div>

      {modal?.type === 'edit' && <CustomerFormModal customer={customer} onClose={() => setModal(null)} onSaved={async () => { setModal(null); await loadCustomer() }} />}
      {modal?.type === 'payment' && <CustomerPaymentModal customer={customer} cashOpen={detail.cajaAbierta} paymentMethods={detail.mediosPago} onClose={() => setModal(null)} onSaved={refreshAccount} />}
      {modal?.type === 'status' && <StatusChangeModal label={customer.nombre} active={customer.activo} submitting={submitting} error={modalError} onClose={() => { setModal(null); setModalError('') }} onConfirm={confirmStatusChange} />}
      {modal?.type === 'cancel-payment' && <CancelPaymentModal payment={modal.payment} submitting={submitting} error={modalError} onClose={() => { setModal(null); setModalError('') }} onConfirm={confirmPaymentCancellation} />}
      {modal?.type === 'detail' && <SaleDetailModal sale={modal.sale} isAdmin={isAdmin} onClose={() => setModal(null)} onReceipt={() => setModal({ type: 'receipt', sale: modal.sale })} onCancel={() => { setModalError(''); setModal({ type: 'cancel-sale', sale: modal.sale }) }} />}
      {modal?.type === 'receipt' && <SaleReceiptModal sale={modal.sale} historical onClose={() => setModal(null)} />}
      {modal?.type === 'cancel-sale' && <CancelSaleModal sale={modal.sale} submitting={submitting} error={modalError} onClose={() => { setModal(null); setModalError('') }} onConfirm={confirmSaleCancellation} />}
    </AppShell>
  )
}

function ContactItem({ icon, label, value }) {
  return <div className="customer-contact-item"><span>{icon}</span><div><small>{label}</small><strong>{value}</strong></div></div>
}

function AccountPanel({ data, loading, isAdmin, actionLoading, onPageChange, onOpenSale, onCancelPayment }) {
  return (
    <section className="customer-detail-panel">
      <header className="customer-detail-panel__header"><div><h2>Movimientos de cuenta</h2><span>{data?.totalElementos || 0} movimientos registrados</span></div></header>
      {loading ? <SectionLoading /> : data?.movimientos.length ? <div className="table-scroll"><table className="account-table"><thead><tr><th>Fecha</th><th>Movimiento</th><th>Descripcion</th><th className="align-right">Importe</th><th className="align-right">Saldo anterior</th><th className="align-right">Saldo resultante</th><th className="align-right">Referencia</th></tr></thead><tbody>{data.movimientos.map((movement) => {
        const negative = ['PAGO_CLIENTE', 'CANCELACION_VENTA'].includes(movement.tipo)
        const canCancel = isAdmin && movement.tipo === 'PAGO_CLIENTE' && movement.pago && !movement.pago.anulado
        return <tr key={movement.id}><td>{dateFormatter.format(new Date(movement.fecha))}</td><td><span className={`movement-type movement-type--${movement.tipo.toLowerCase()}`}>{movementLabel(movement.tipo)}</span></td><td className="movement-description">{movement.descripcion}</td><td className={`align-right movement-amount ${negative ? 'movement-amount--credit' : 'movement-amount--debit'}`}>{negative ? '-' : '+'}{moneyFormatter.format(Number(movement.importe))}</td><td className="align-right">{moneyFormatter.format(Number(movement.saldoAnterior))}</td><td className="align-right money-cell">{moneyFormatter.format(Number(movement.saldoPosterior))}</td><td><div className="movement-actions">{movement.ventaId && <button className="icon-button" type="button" title={`Ver venta #${movement.ventaId}`} aria-label={`Ver venta ${movement.ventaId}`} disabled={Boolean(actionLoading)} onClick={() => onOpenSale(movement.ventaId)}><ReceiptText size={17} /></button>}{movement.pago && <span>Pago #{movement.pago.id}</span>}{canCancel && <button className="icon-button movement-cancel" type="button" title="Anular cobro" aria-label={`Anular cobro ${movement.pago.id}`} onClick={() => onCancelPayment(movement.pago)}><Ban size={17} /></button>}</div></td></tr>
      })}</tbody></table></div> : <div className="catalog-empty"><WalletCards size={34} /><strong>La cuenta no tiene movimientos</strong><span>Las ventas a cuenta corriente y los cobros apareceran aqui.</span></div>}
      {data && data.totalPaginas > 1 && <Pagination data={data} loading={loading} onChange={onPageChange} />}
    </section>
  )
}

function SalesPanel({ data, loading, actionLoading, onPageChange, onOpenSale }) {
  return (
    <section className="customer-detail-panel">
      <header className="customer-detail-panel__header"><div><h2>Ventas del cliente</h2><span>{data?.totalElementos || 0} ventas registradas</span></div></header>
      {loading ? <SectionLoading /> : data?.ventas.length ? <div className="table-scroll"><table className="customer-sales-table"><thead><tr><th>Venta</th><th>Fecha</th><th>Vendedor</th><th>Pago</th><th>Estado</th><th className="align-right">Total</th><th className="align-right">Accion</th></tr></thead><tbody>{data.ventas.map((sale) => <tr key={sale.id}><td><strong>#{sale.id}</strong></td><td>{dateFormatter.format(new Date(sale.fecha))}</td><td>{sale.vendedor}</td><td>{formatLabel(sale.formaPago)}</td><td><span className={sale.estado === 'CANCELADA' ? 'sale-status sale-status--canceled' : 'sale-status'}>{formatLabel(sale.estado)}</span></td><td className="align-right money-cell">{moneyFormatter.format(Number(sale.total))}</td><td><div className="customer-actions"><button className="icon-button customer-action--view" type="button" title="Ver detalle" aria-label={`Ver venta ${sale.id}`} disabled={Boolean(actionLoading)} onClick={() => onOpenSale(sale.id)}><Eye size={17} /></button><button className="icon-button" type="button" title="Ver comprobante" aria-label={`Ver comprobante ${sale.id}`} disabled={Boolean(actionLoading)} onClick={() => onOpenSale(sale.id, 'receipt')}><FileText size={17} /></button></div></td></tr>)}</tbody></table></div> : <div className="catalog-empty"><ReceiptText size={34} /><strong>El cliente no tiene ventas</strong><span>Sus comprobantes apareceran aqui cuando realices una venta.</span></div>}
      {data && data.totalPaginas > 1 && <Pagination data={data} loading={loading} onChange={onPageChange} />}
    </section>
  )
}

function Pagination({ data, loading, onChange }) {
  return <nav className="history-pagination" aria-label="Paginacion"><button className="icon-button" type="button" aria-label="Pagina anterior" disabled={data.primera || loading} onClick={() => onChange(data.pagina - 1)}><ChevronLeft size={19} /></button><span>Pagina {data.pagina + 1} de {data.totalPaginas}</span><button className="icon-button" type="button" aria-label="Pagina siguiente" disabled={data.ultima || loading} onClick={() => onChange(data.pagina + 1)}><ChevronRight size={19} /></button></nav>
}

function CustomerDetailLoading() {
  return <div className="customer-detail-loading" role="status"><div className="skeleton" /><div className="skeleton" /><div className="skeleton" /></div>
}

function SectionLoading() {
  return <div className="history-loading" role="status"><div className="skeleton" /><div className="skeleton" /><div className="skeleton" /></div>
}

function balanceLabel(state) {
  if (state === 'DEUDA') return 'Deuda pendiente'
  if (state === 'A_FAVOR') return 'Saldo a favor'
  return 'Cuenta al dia'
}

function movementLabel(type) {
  const labels = { VENTA_CUENTA_CORRIENTE: 'Venta', PAGO_CLIENTE: 'Cobro', CANCELACION_VENTA: 'Venta cancelada', ANULACION_PAGO_CLIENTE: 'Cobro anulado' }
  return labels[type] || formatLabel(type)
}

function formatLabel(value) {
  return String(value || '').toLowerCase().replaceAll('_', ' ').replace(/^./, (letter) => letter.toUpperCase())
}
