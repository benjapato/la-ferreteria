import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  AlertTriangle, Banknote, Building2, CreditCard, Minus, PackagePlus, Plus,
  History, Search, ShoppingCart, Trash2, UserPlus, WalletCards, X, Printer, FileText, Settings,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { apiRequest } from '../api/client'
import { useAuth } from '../auth/useAuth'
import AppShell from '../components/AppShell'
import CashOpeningModal from '../components/CashOpeningModal'
import QuickClientModal from '../components/QuickClientModal'
import QuickProductModal from '../components/QuickProductModal'
import SaleReceiptModal from '../components/SaleReceiptModal'
import SaleConfirmationModal from '../components/SaleConfirmationModal'
import PrinterSettingsModal from '../components/PrinterSettingsModal'
import { sendTicket } from '../utils/ticketPrinting'
import { allowsFraction, availableQuantity, lineSubtotal, minimumQuantity, quantityValue, validQuantity } from '../utils/quantities'

const moneyFormatter = new Intl.NumberFormat('es-AR', {
  style: 'currency', currency: 'ARS', minimumFractionDigits: 2,
})

const paymentOptions = [
  { value: 'EFECTIVO', label: 'Efectivo', icon: Banknote },
  { value: 'TRANSFERENCIA', label: 'Transferencia', icon: Building2 },
  { value: 'TARJETA_DEBITO', label: 'Debito', icon: CreditCard },
  { value: 'TARJETA_CREDITO', label: 'Credito', icon: CreditCard },
  { value: 'COMBINADO', label: 'Combinado', icon: WalletCards },
  { value: 'CUENTA_CORRIENTE', label: 'Cuenta corriente', icon: UserPlus },
]

const combinedMethods = paymentOptions.slice(0, 4)
const emptyCombined = { EFECTIVO: '', TRANSFERENCIA: '', TARJETA_DEBITO: '', TARJETA_CREDITO: '' }

const formatMoney = (value) => moneyFormatter.format(Number(value || 0))
const formatLabel = (value) => String(value || '').toLowerCase().replaceAll('_', ' ').replace(/^./, (letter) => letter.toUpperCase())

function SalesSkeleton() {
  return (
    <div className="sales-skeleton" role="status" aria-label="Cargando nueva venta">
      <div className="skeleton skeleton--title" />
      <div className="sales-skeleton__grid"><div className="skeleton" /><div className="skeleton" /><div className="skeleton" /></div>
    </div>
  )
}

export default function SalesPage() {
  const { user } = useAuth()
  const isAdmin = user.rol === 'ADMINISTRADOR'
  const [setup, setSetup] = useState(null)
  const [products, setProducts] = useState([])
  const [clients, setClients] = useState([])
  const [loading, setLoading] = useState(true)
  const [productQuery, setProductQuery] = useState('')
  const [clientQuery, setClientQuery] = useState('')
  const [clientResultsOpen, setClientResultsOpen] = useState(false)
  const [cart, setCart] = useState([])
  const [selectedClient, setSelectedClient] = useState(null)
  const [manualClientName, setManualClientName] = useState('')
  const [paymentMethod, setPaymentMethod] = useState('EFECTIVO')
  const [combinedAmounts, setCombinedAmounts] = useState(emptyCombined)
  const [observations, setObservations] = useState('')
  const [modal, setModal] = useState(null)
  const [receipt, setReceipt] = useState(null)
  const [receiptFormat, setReceiptFormat] = useState('ticket')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [review, setReview] = useState(null)
  const [reviewError, setReviewError] = useState('')
  const clientPickerRef = useRef(null)
  const clientInputRef = useRef(null)
  const submissionRef = useRef(false)
  const salesPageRef = useRef(null)
  const keyboardNavigationRef = useRef(false)

  const loadSetup = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const data = await apiRequest('/api/sales/setup')
      setSetup(data)
      setProducts(data.productos)
      setClients(data.clientes)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadSetup() }, [loadSetup])

  useEffect(() => {
    if (!clientResultsOpen) return undefined
    function dismissOutside(event) {
      if (!clientPickerRef.current?.contains(event.target)) setClientResultsOpen(false)
    }
    document.addEventListener('pointerdown', dismissOutside)
    return () => document.removeEventListener('pointerdown', dismissOutside)
  }, [clientResultsOpen])

  useEffect(() => {
    if (!setup) return undefined
    let active = true
    const timeout = window.setTimeout(async () => {
      try {
        const result = await apiRequest(`/api/sales/products?query=${encodeURIComponent(productQuery)}`)
        if (active) setProducts(result)
      } catch (requestError) {
        if (active) setError(requestError.message)
      }
    }, 220)
    return () => { active = false; window.clearTimeout(timeout) }
  }, [productQuery, setup])

  useEffect(() => {
    if (!setup || selectedClient) return undefined
    let active = true
    const timeout = window.setTimeout(async () => {
      try {
        const result = await apiRequest(`/api/sales/clients?query=${encodeURIComponent(clientQuery)}`)
        if (active) setClients(result)
      } catch (requestError) {
        if (active) setError(requestError.message)
      }
    }, 220)
    return () => { active = false; window.clearTimeout(timeout) }
  }, [clientQuery, selectedClient, setup])

  const total = useMemo(
    () => cart.reduce((cents, item) => cents + Math.round(lineSubtotal(item.product.precio, item.quantity) * 100), 0) / 100,
    [cart],
  )

  const combinedSummary = useMemo(() => {
    const positive = Object.values(combinedAmounts).map(Number).filter((amount) => Number.isFinite(amount) && amount > 0)
    const assigned = positive.reduce((sum, amount) => sum + amount, 0)
    return { assigned, count: positive.length, difference: total - assigned }
  }, [combinedAmounts, total])

  const combinedUsesAccount = selectedClient !== null && Math.abs(combinedSummary.difference) >= 0.005
  const estimatedClientBalance = selectedClient
    ? Number(selectedClient.saldo || 0) + (combinedUsesAccount ? combinedSummary.difference : 0)
    : null
  const combinedValid = paymentMethod !== 'COMBINADO'
    || (Object.values(combinedAmounts).every((value) => {
      const amount = Number(value)
      return Number.isFinite(amount) && amount >= 0 && Math.abs(amount * 100 - Math.round(amount * 100)) < 0.000001
    }) && (selectedClient
      ? combinedSummary.count >= 1 && (combinedUsesAccount || combinedSummary.count >= 2)
      : combinedSummary.count >= 2 && Math.abs(combinedSummary.difference) < 0.005))
  const accountValid = paymentMethod !== 'CUENTA_CORRIENTE' || selectedClient !== null
  const cartValid = cart.length > 0 && cart.every((item) => validQuantity(item.quantity, item.product))
  const canSubmit = Boolean(setup?.caja) && cartValid && total > 0 && combinedValid && accountValid && !submitting

  function addProduct(product) {
    const stock = availableQuantity(product)
    if (stock < minimumQuantity(product)) return
    setCart((current) => {
      const existing = current.find((item) => item.product.id === product.id)
      if (!existing) return [...current, { product, quantity: Math.min(1, stock) }]
      return current.map((item) => item.product.id === product.id
        ? { ...item, quantity: Number(Math.min(stock, (quantityValue(item.quantity) || 0) + 1).toFixed(3)) }
        : item)
    })
  }

  function updateQuantity(productId, value) {
    setCart((current) => current.map((item) => {
      if (item.product.id !== productId) return item
      const rawValue = String(value)
      return /^\d*(?:[.,]\d*)?$/.test(rawValue) ? { ...item, quantity: rawValue } : item
    }))
  }

  function adjustQuantity(productId, change) {
    setCart((current) => current.map((item) => {
      if (item.product.id !== productId) return item
      const quantity = Number(Math.min(availableQuantity(item.product), Math.max(minimumQuantity(item.product), (quantityValue(item.quantity) || 0) + change)).toFixed(3))
      return { ...item, quantity }
    }))
  }

  function normalizeQuantity(productId) {
    setCart((current) => current.map((item) => item.product.id === productId && item.quantity === ''
      ? { ...item, quantity: Math.min(1, availableQuantity(item.product)) }
      : item))
  }

  function removeProduct(productId) {
    setCart((current) => current.filter((item) => item.product.id !== productId))
  }

  async function refreshAfterSale() {
    const data = await apiRequest('/api/sales/setup')
    setSetup(data)
    setProducts(data.productos)
    setClients(data.clientes)
  }

  const reviewSale = useCallback(() => {
    setError('')
    if (!canSubmit) {
      setError(!setup?.caja ? 'Debe abrir la caja antes de registrar ventas.' : 'Revise el carrito, el cliente y los importes del pago.')
      return
    }
    const payments = paymentMethod === 'COMBINADO'
      ? combinedMethods.map(({ value }) => ({ formaPago: value, monto: Number(combinedAmounts[value]) })).filter((payment) => payment.monto > 0)
      : []
    const accountDelta = paymentMethod === 'CUENTA_CORRIENTE' ? total
      : paymentMethod === 'COMBINADO' ? Number(combinedSummary.difference.toFixed(2)) : 0
    const paymentBreakdown = paymentMethod === 'COMBINADO'
      ? [...payments, ...(accountDelta !== 0 ? [{ formaPago: 'CUENTA_CORRIENTE', monto: accountDelta }] : [])]
      : [{ formaPago: paymentMethod, monto: total }]
    setReview({
      formatoComprobante: receiptFormat,
      cliente: selectedClient?.nombre || manualClientName.trim() || 'Consumidor final',
      items: cart.map((item) => ({ ...item.product, cantidad: quantityValue(item.quantity) })),
      total,
      pagos: paymentBreakdown,
      saldoAnterior: selectedClient && accountDelta !== 0 ? Number(selectedClient.saldo || 0) : null,
      saldoPosterior: selectedClient && accountDelta !== 0 ? Number(selectedClient.saldo || 0) + accountDelta : null,
      body: {
        clienteId: selectedClient?.id || null,
        nombreCliente: selectedClient ? '' : manualClientName.trim(),
        formaPago: paymentMethod,
        observaciones: observations.trim(),
        detalles: cart.map((item) => ({ productoId: item.product.id, cantidad: quantityValue(item.quantity) })),
        pagos: payments,
      },
    })
    setClientResultsOpen(false)
    setReviewError('')
  }, [canSubmit, setup?.caja, paymentMethod, combinedAmounts, combinedSummary.difference, total, selectedClient, manualClientName, cart, observations, receiptFormat])

  useEffect(() => {
    function handlePointer() { keyboardNavigationRef.current = false }
    function handleEnter(event) {
      if (event.key === 'Tab' || event.key.startsWith('Arrow')) {
        keyboardNavigationRef.current = true
        return
      }
      if (event.key !== 'Enter' || event.defaultPrevented || event.isComposing
        || event.ctrlKey || event.altKey || event.metaKey || event.shiftKey) return
      if (!canSubmit || document.querySelector('[role="dialog"]')) return
      const target = event.target
      if (!(target instanceof HTMLElement)) return
      if (target !== document.body && !salesPageRef.current?.contains(target)) return
      if (target.closest('textarea, [contenteditable="true"], a, select')) return
      if (clientResultsOpen && clientPickerRef.current?.contains(target)) return
      // Los botones elegidos con Tab conservan su accion habitual con Enter.
      if (keyboardNavigationRef.current && target.closest('button')) return
      event.preventDefault()
      if (event.repeat) return
      if (salesPageRef.current?.querySelector('.sale-workspace')?.checkValidity()) reviewSale()
    }
    document.addEventListener('pointerdown', handlePointer)
    document.addEventListener('keydown', handleEnter)
    return () => {
      document.removeEventListener('pointerdown', handlePointer)
      document.removeEventListener('keydown', handleEnter)
    }
  }, [canSubmit, clientResultsOpen, reviewSale])

  const closeReview = useCallback(() => {
    if (!submissionRef.current) setReview(null)
  }, [])

  async function confirmSale() {
    if (!review || submissionRef.current) return
    submissionRef.current = true
    setSubmitting(true)
    setReviewError('')
    try {
      const sale = await apiRequest('/api/sales', {
        method: 'POST',
        body: review.body,
      })
      setReview(null)
      // La impresion ocurre una vez, despues de guardar; nunca desde un efecto de React.
      const printPromise = review.formatoComprobante === 'ticket' ? sendTicket(sale.id) : null
      setReceipt({ ...sale, formatoComprobante: review.formatoComprobante, printPromise })
      setReceiptFormat('ticket')
      setCart([])
      setSelectedClient(null)
      setClientQuery('')
      setManualClientName('')
      setPaymentMethod('EFECTIVO')
      setCombinedAmounts(emptyCombined)
      setObservations('')
      try {
        await refreshAfterSale()
      } catch {
        setError('La venta se registro, pero no se pudo actualizar el catalogo. Recarga la pantalla antes de continuar.')
      }
    } catch (requestError) {
      setReviewError(requestError.message)
    } finally {
      submissionRef.current = false
      setSubmitting(false)
    }
  }

  function handleClientCreated(client) {
    setClients((current) => [client, ...current.filter((item) => item.id !== client.id)])
    setSelectedClient(client)
    setClientQuery('')
    setClientResultsOpen(false)
    setModal(null)
  }

  function handleProductCreated(product) {
    setProducts((current) => [product, ...current.filter((item) => item.id !== product.id)])
    addProduct(product)
    setProductQuery('')
    setModal(null)
  }

  return (
    <AppShell onCashChanged={loadSetup} cashStatusKey={setup?.caja?.id || ''}>
      {loading ? <SalesSkeleton /> : (
        <div className="sales-page" ref={salesPageRef}>
          <div className="page-heading sales-heading">
            <div><p className="eyebrow">Caja y mostrador</p><h1>Nueva venta</h1><p>Agrega productos, elige el cobro y confirma la operacion.</p></div>
            <div className="sales-heading__actions">
              <Link className="secondary-button" to="/ventas"><History size={17} />Historial</Link>
              <div className={`cash-pill ${setup?.caja ? 'cash-pill--open' : 'cash-pill--closed'}`}><span />{setup?.caja ? `Caja #${setup.caja.id} abierta` : 'Caja cerrada'}</div>
            </div>
          </div>

          {!setup?.caja && (
            <div className="cash-warning" role="alert">
              <AlertTriangle size={22} />
              <div><strong>La caja esta cerrada</strong><p>Es necesario abrirla antes de confirmar una venta.</p></div>
              {isAdmin
                ? <button className="primary-button" type="button" onClick={() => setModal('cash')}><Banknote size={18} />Abrir caja</button>
                : <Link className="secondary-button" to="/"><Banknote size={18} />Abrir caja desde Inicio</Link>}
            </div>
          )}

          {error && <div className="sale-error" role="alert"><AlertTriangle size={19} /><span>{error}</span><button className="icon-button" type="button" aria-label="Cerrar error" onClick={() => setError('')}><X size={18} /></button></div>}

          <form className="sale-workspace" onSubmit={(event) => event.preventDefault()}>
            <section className="sale-panel product-browser">
              <header className="sale-panel__header"><div><h2>Productos</h2><span>{products.length} resultados</span></div>{isAdmin && <button className="icon-text-button" type="button" onClick={() => setModal('product')}><PackagePlus size={18} />Nuevo</button>}</header>
              <div className="sale-search"><Search size={18} /><input aria-label="Buscar producto por nombre" placeholder="Buscar por nombre" value={productQuery} onChange={(event) => setProductQuery(event.target.value)} /></div>
              <div className="product-results">
                {products.length ? products.map((product) => {
                  const outOfStock = availableQuantity(product) < minimumQuantity(product)
                  return (
                    <article className="product-result" key={product.id}>
                      <div className="product-result__code">{product.codigo}</div>
                      <div className="product-result__info"><strong>{product.nombre}</strong><span>{product.marca || product.categoria}</span><small className={outOfStock ? 'stock-zero' : ''}>{outOfStock ? 'Sin stock' : `Stock: ${product.stock} ${formatLabel(product.unidadMedida)}`}</small></div>
                      <div className="product-result__action"><b>{formatMoney(product.precio)}</b><button className="icon-button add-product" type="button" title="Agregar al carrito" aria-label={`Agregar ${product.nombre}`} disabled={outOfStock} onClick={() => addProduct(product)}><Plus size={19} /></button></div>
                    </article>
                  )
                }) : <div className="compact-empty"><Search size={25} /><strong>No encontramos productos</strong><span>Cambia la busqueda o crea uno nuevo.</span></div>}
              </div>
            </section>

            <section className="sale-panel cart-panel">
              <header className="sale-panel__header"><div><h2>Carrito</h2><span>{cart.length} productos</span></div>{cart.length > 0 && <button className="text-danger-button" type="button" onClick={() => setCart([])}>Vaciar</button>}</header>
              {cart.length ? (
                <div className="cart-content">
                  <div className="cart-table-wrap">
                    <table className="cart-table">
                      <thead><tr><th>Producto</th><th>Cantidad</th><th>Precio</th><th className="align-right">Subtotal</th><th aria-label="Acciones" /></tr></thead>
                      <tbody>{cart.map((item) => (
                        <tr key={item.product.id}>
                          <td><strong>{item.product.nombre}</strong><small>{item.product.codigo} · Stock {item.product.stock} {formatLabel(item.product.unidadMedida)}</small></td>
                          <td><div className="quantity-control"><button type="button" aria-label={`Restar ${item.product.nombre}`} title="Restar" disabled={quantityValue(item.quantity) <= minimumQuantity(item.product)} onClick={() => adjustQuantity(item.product.id, -1)}><Minus size={15} /></button><input aria-label={`Cantidad de ${item.product.nombre}`} type="text" inputMode={allowsFraction(item.product) ? 'decimal' : 'numeric'} title={allowsFraction(item.product) ? 'Cantidad con hasta 3 decimales' : 'Cantidad entera'} aria-invalid={!validQuantity(item.quantity, item.product)} value={item.quantity} onChange={(event) => updateQuantity(item.product.id, event.target.value)} onBlur={() => normalizeQuantity(item.product.id)} /><button type="button" aria-label={`Sumar ${item.product.nombre}`} title="Sumar" disabled={quantityValue(item.quantity) >= availableQuantity(item.product)} onClick={() => adjustQuantity(item.product.id, 1)}><Plus size={15} /></button></div></td>
                          <td>{formatMoney(item.product.precio)}</td>
                          <td className="align-right cart-subtotal">{formatMoney(lineSubtotal(item.product.precio, item.quantity))}</td>
                          <td><button className="icon-button remove-item" type="button" title="Quitar" aria-label={`Quitar ${item.product.nombre}`} onClick={() => removeProduct(item.product.id)}><Trash2 size={17} /></button></td>
                        </tr>
                      ))}</tbody>
                    </table>
                  </div>
                  <div className="cart-total"><span>Total de productos</span><strong>{formatMoney(total)}</strong></div>
                </div>
              ) : <div className="cart-empty"><ShoppingCart size={34} /><strong>El carrito esta vacio</strong><span>Agrega productos desde el catalogo.</span></div>}
            </section>

            <section className="sale-panel checkout-panel">
              <header className="sale-panel__header"><div><h2>Cobro</h2><span>Cliente y forma de pago</span></div></header>
              <div className="checkout-content">
                <div className="checkout-section">
                  <div className="section-label"><span>Cliente</span><button className="icon-text-button" type="button" onClick={() => setModal('client')}><UserPlus size={17} />Nuevo</button></div>
                  {selectedClient ? (
                    <div className={`selected-client selected-client--${balanceTone(selectedClient.saldo)}`}><div><strong>{selectedClient.nombre}</strong><span>{selectedClient.documento || 'Sin documento'} · {clientBalance(selectedClient.saldo)}</span></div><button className="icon-button" type="button" title="Quitar cliente" aria-label="Quitar cliente" onClick={() => setSelectedClient(null)}><X size={17} /></button></div>
                  ) : (
                    <div className="client-picker" ref={clientPickerRef}
                      onPointerLeave={(event) => { if (event.pointerType === 'mouse') setClientResultsOpen(false) }}
                      onBlur={(event) => { if (!event.currentTarget.contains(event.relatedTarget)) setClientResultsOpen(false) }}
                      onKeyDown={(event) => {
                        if (event.key === 'Escape') {
                          event.stopPropagation()
                          clientInputRef.current?.focus()
                          setClientResultsOpen(false)
                        }
                      }}>
                      <div className="sale-search"><Search size={17} /><input ref={clientInputRef} aria-label="Buscar cliente" aria-expanded={clientResultsOpen} aria-controls="sale-client-results" placeholder="Nombre o documento" value={clientQuery} onFocus={() => setClientResultsOpen(true)} onClick={() => setClientResultsOpen(true)} onPointerEnter={(event) => { if (document.activeElement === event.currentTarget) setClientResultsOpen(true) }} onChange={(event) => { setClientQuery(event.target.value); setClientResultsOpen(true) }} /></div>
                      {clientResultsOpen && (
                        <div className="client-results" id="sale-client-results">
                          {clients.length ? clients.map((client) => <button className={`client-result client-result--${balanceTone(client.saldo)}`} type="button" key={client.id} onClick={() => { setSelectedClient(client); setClientResultsOpen(false); setManualClientName('') }}><strong>{client.nombre}</strong><span>{client.documento || 'Sin documento'} · {clientBalance(client.saldo)}</span></button>) : <span>No se encontraron clientes.</span>}
                        </div>
                      )}
                    </div>
                  )}
                  {!selectedClient && paymentMethod !== 'CUENTA_CORRIENTE' && <div className="form-field manual-client"><label htmlFor="manual-client">Nombre opcional</label><input id="manual-client" value={manualClientName} onChange={(event) => setManualClientName(event.target.value)} maxLength="150" placeholder="Consumidor final" /></div>}
                  <div className="receipt-choice">
                    <div className="section-label receipt-choice__heading"><span>Comprobante</span>{isAdmin && <button className="printer-config-button" type="button" title="Configurar impresora de tickets" aria-label="Configurar impresora de tickets" onClick={() => setModal('printer')}><Settings size={17} /><span>Configurar impresora</span></button>}</div>
                    <div className="receipt-modes" role="group" aria-label="Tipo de comprobante">
                      <button type="button" aria-pressed={receiptFormat === 'ticket'} onClick={() => setReceiptFormat('ticket')}><Printer size={17} />Ticket</button>
                      <button type="button" aria-pressed={receiptFormat === 'a4'} onClick={() => setReceiptFormat('a4')}><FileText size={17} />Factura (A4)</button>
                    </div>
                  </div>
                </div>

                <div className="checkout-section">
                  <span className="section-title">Forma de pago</span>
                  <div className="payment-grid">{paymentOptions.map(({ value, label, icon: Icon }) => <button className={paymentMethod === value ? 'payment-option payment-option--active' : 'payment-option'} type="button" key={value} aria-pressed={paymentMethod === value} onClick={() => setPaymentMethod(value)}><Icon size={18} /><span>{label}</span></button>)}</div>
                </div>

                {paymentMethod === 'COMBINADO' && (
                  <div className="combined-payment">
                    {combinedMethods.map(({ value, label }) => <label key={value}><span>{label}</span><div className="money-input"><span>$</span><input aria-label={`Importe ${label}`} type="number" min="0" step="0.01" value={combinedAmounts[value]} onChange={(event) => setCombinedAmounts((current) => ({ ...current, [value]: event.target.value }))} /></div></label>)}
                    <div className={`combined-account ${selectedClient ? `combined-account--${balanceTone(estimatedClientBalance)}` : ''}`}>
                      <UserPlus size={18} />
                      <div><span>Cuenta corriente</span><strong>{selectedClient ? combinedAccountLabel(combinedSummary.difference) : 'Selecciona un cliente para utilizarla'}</strong></div>
                      {selectedClient && <small>Saldo estimado: {clientBalance(estimatedClientBalance)}</small>}
                    </div>
                    <div className={combinedValid ? 'combined-status combined-status--valid' : 'combined-status'}><span>Recibido: {formatMoney(combinedSummary.assigned)}</span><strong>{combinedMessage(combinedSummary, selectedClient, combinedValid)}</strong></div>
                  </div>
                )}

                {paymentMethod === 'CUENTA_CORRIENTE' && !selectedClient && <p className="checkout-alert"><AlertTriangle size={16} />Selecciona o crea un cliente para usar cuenta corriente.</p>}

                <div className="form-field"><label htmlFor="sale-observations">Observaciones</label><textarea id="sale-observations" value={observations} onChange={(event) => setObservations(event.target.value)} maxLength="500" rows="2" /></div>
              </div>
              <footer className="checkout-footer"><div><span>Total a cobrar</span><strong>{formatMoney(total)}</strong></div><button className="confirm-sale-button" type="button" onClick={reviewSale} disabled={!canSubmit}><ShoppingCart size={20} />Revisar venta</button></footer>
            </section>
          </form>
        </div>
      )}

      {modal === 'cash' && <CashOpeningModal onClose={() => setModal(null)} onOpened={(cash) => { setSetup((current) => ({ ...current, caja: cash })); setModal(null) }} />}
      {modal === 'client' && <QuickClientModal onClose={() => setModal(null)} onCreated={handleClientCreated} />}
      {modal === 'printer' && <PrinterSettingsModal onClose={() => setModal(null)} />}
      {modal === 'product' && <QuickProductModal categories={setup.categorias} units={setup.unidadesMedida} onClose={() => setModal(null)} onCreated={handleProductCreated} onCategoryCreated={(category) => setSetup((current) => ({ ...current, categorias: [...current.categorias, category] }))} />}
      {receipt && <SaleReceiptModal sale={receipt} onClose={() => setReceipt(null)} />}
      {review && <SaleConfirmationModal sale={review} submitting={submitting} error={reviewError} onClose={closeReview} onConfirm={confirmSale} />}
    </AppShell>
  )
}

function clientBalance(value) {
  const balance = Number(value || 0)
  if (balance > 0.005) return `Debe ${formatMoney(balance)}`
  if (balance < -0.005) return `A favor ${formatMoney(Math.abs(balance))}`
  return 'Cuenta al dia'
}

function balanceTone(value) {
  const balance = Number(value || 0)
  if (balance > 0.005) return 'debt'
  if (balance < -0.005) return 'credit'
  return 'current'
}

function combinedAccountLabel(difference) {
  if (difference > 0.005) return `Se suma una deuda de ${formatMoney(difference)}`
  if (difference < -0.005) return `Queda a favor ${formatMoney(Math.abs(difference))}`
  return 'Sin ajuste de saldo'
}

function combinedMessage(summary, selectedClient, valid) {
  if (summary.count === 0) return 'Carga al menos un medio de cobro'
  if (!selectedClient && summary.count < 2) return 'Usa al menos dos medios'
  if (!selectedClient && Math.abs(summary.difference) >= 0.005) return 'El importe debe completar el total'
  if (selectedClient && Math.abs(summary.difference) < 0.005 && summary.count < 2) return 'Usa otro medio o cambia la forma de pago'
  if (valid && summary.difference > 0.005) return 'La diferencia se suma a la deuda'
  if (valid && summary.difference < -0.005) return 'La diferencia queda a favor'
  return valid ? 'Pago completo' : 'Revisa los importes'
}
