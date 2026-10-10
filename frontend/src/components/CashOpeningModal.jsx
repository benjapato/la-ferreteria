import { useEffect, useState } from 'react'
import { AlertTriangle, Banknote, RefreshCw } from 'lucide-react'
import { apiRequest } from '../api/client'
import Modal from './Modal'

export default function CashOpeningModal({ endpoint = '/api/sales/cash/open', onClose, onOpened }) {
  const [amount, setAmount] = useState('0')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [openingStatus, setOpeningStatus] = useState(null)
  const [checking, setChecking] = useState(true)
  const [statusError, setStatusError] = useState('')
  const [checkVersion, setCheckVersion] = useState(0)
  const [acknowledged, setAcknowledged] = useState(false)
  const shortage = openingStatus?.faltanteUltimoCierre
  const blocked = checking || Boolean(statusError) || Boolean(openingStatus?.arqueoPendiente) || (Boolean(shortage) && !acknowledged)

  useEffect(() => {
    let active = true
    setChecking(true)
    setStatusError('')
    setAcknowledged(false)
    apiRequest('/api/cash/status').then((status) => {
      if (active) setOpeningStatus(status)
    }).catch(() => {
      if (active) setStatusError('No se pudo comprobar el cierre anterior. Intenta nuevamente antes de abrir la caja.')
    }).finally(() => { if (active) setChecking(false) })
    return () => { active = false }
  }, [checkVersion])

  async function handleSubmit(event) {
    event.preventDefault()
    if (blocked || submitting) return
    setSubmitting(true)
    setError('')
    try {
      const cash = await apiRequest(endpoint, {
        method: 'POST',
        body: { montoInicial: Number(amount || 0) },
      })
      onOpened(cash)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      title="Abrir caja"
      description="Indica el efectivo disponible al comenzar el turno."
      onClose={onClose}
      footer={(
        <>
          <button className="secondary-button" type="button" onClick={onClose} disabled={submitting}>Cancelar</button>
          <button className="primary-button" type="submit" form="cash-opening-form" disabled={submitting || blocked}>
            <Banknote size={18} />{submitting ? 'Abriendo...' : 'Abrir caja'}
          </button>
        </>
      )}
    >
      <form id="cash-opening-form" onSubmit={handleSubmit}>
        {checking && <p role="status">Comprobando el cierre anterior...</p>}
        {statusError && <div className="cash-opening-error" role="alert"><p>{statusError}</p><button className="secondary-button" type="button" onClick={() => setCheckVersion((value) => value + 1)}><RefreshCw size={16} />Reintentar</button></div>}
        {!checking && openingStatus?.arqueoPendiente && <p className="form-error" role="alert">Un administrador debe completar el arqueo pendiente antes de abrir otra caja.</p>}
        {!checking && shortage && <div className="cash-shortage" role="alert">
          <div className="cash-shortage__heading"><AlertTriangle size={21} /><strong>Falto efectivo en el ultimo cierre</strong></div>
          <p>Caja #{shortage.id} · {new Intl.DateTimeFormat('es-AR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(shortage.fechaCierre))}</p>
          <dl><div><dt>Esperado</dt><dd>{formatMoney(shortage.efectivoEsperado)}</dd></div><div><dt>Contado</dt><dd>{formatMoney(shortage.efectivoContado)}</dd></div><div><dt>Faltante</dt><dd>{formatMoney(shortage.faltante)}</dd></div></dl>
          <label className="cash-shortage__ack"><input type="checkbox" checked={acknowledged} onChange={(event) => setAcknowledged(event.target.checked)} />Tome conocimiento del faltante</label>
        </div>}
        <label className="field-label" htmlFor="initial-amount">Efectivo inicial</label>
        <div className="money-input"><span>$</span><input id="initial-amount" type="number" min="0" step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} autoFocus /></div>
        <p className="field-help">Puede ser cero si la caja comienza sin cambio.</p>
        {error && <p className="form-error" role="alert">{error}</p>}
      </form>
    </Modal>
  )
}

function formatMoney(value) {
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(Number(value || 0))
}
