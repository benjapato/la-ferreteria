import { Calculator, LockKeyhole } from 'lucide-react'
import { useRef, useState } from 'react'
import { apiRequest } from '../api/client'
import Modal from './Modal'

const moneyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', minimumFractionDigits: 2 })
const formatMoney = (value) => moneyFormatter.format(Number(value || 0))

export default function CashCloseModal({ cash, mode = 'close', onClose, onClosed }) {
  const [counted, setCounted] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const submittingRef = useRef(false)
  const countedAmount = Number(counted || 0)
  const validAmount = Number.isFinite(countedAmount) && countedAmount >= 0
    && Math.abs(countedAmount * 100 - Math.round(countedAmount * 100)) < 0.000001
  const difference = validAmount ? countedAmount - Number(cash.arqueo.efectivoEsperado || 0) : null
  const reconciliation = mode === 'reconcile'

  async function submit(event) {
    event.preventDefault()
    if (submittingRef.current || !validAmount) return
    submittingRef.current = true
    setSubmitting(true)
    setError('')
    try {
      const endpoint = reconciliation ? `/api/cash/${cash.id}/reconciliation` : '/api/cash/close'
      const closed = await apiRequest(endpoint, { method: 'POST', body: { efectivoContado: countedAmount } })
      onClosed(closed)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      submittingRef.current = false
      setSubmitting(false)
    }
  }

  return (
    <Modal title={reconciliation ? `Completar arqueo de caja #${cash.id}` : `Cerrar caja #${cash.id}`} description={reconciliation ? 'Ingresa el efectivo que quedo al momento del cierre automatico.' : 'Conta el efectivo fisico antes de confirmar el cierre.'} onClose={() => { if (!submittingRef.current) onClose() }} footer={<><button className="secondary-button" type="button" onClick={onClose} disabled={submitting}>Cancelar</button><button className={reconciliation ? 'primary-button' : 'danger-button'} type="submit" form="cash-close-form" disabled={submitting || !validAmount}>{reconciliation ? <Calculator size={18} /> : <LockKeyhole size={18} />}{submitting ? 'Guardando...' : reconciliation ? 'Completar arqueo' : 'Cerrar caja'}</button></>}>
      <form id="cash-close-form" className="cash-close-form" onSubmit={submit}>
        <div className="cash-close-expected"><Calculator size={23} /><div><span>Efectivo esperado</span><strong>{formatMoney(cash.arqueo.efectivoEsperado)}</strong></div></div>
        <div className="form-field"><label htmlFor="cash-counted">{reconciliation ? 'Efectivo contado al cierre' : 'Efectivo contado'}</label><input id="cash-counted" type="number" min="0" step="0.01" placeholder="0" value={counted} onChange={(event) => setCounted(event.target.value)} autoFocus /></div>
        <div className={`cash-close-difference ${difference == null ? '' : difference < 0 ? 'cash-close-difference--negative' : difference > 0 ? 'cash-close-difference--positive' : ''}`}><span>Diferencia</span><strong>{difference == null ? '-' : formatMoney(difference)}</strong></div>
        <p className="cash-close-note">{reconciliation ? 'Al completar el arqueo se calculara la diferencia y podras abrir una caja nueva.' : 'Una vez cerrada, no se podran registrar nuevos movimientos ni cancelar cobros asociados a esta caja.'}</p>
        {error && <p className="form-error" role="alert">{error}</p>}
      </form>
    </Modal>
  )
}
