import { useEffect, useRef, useState } from 'react'
import { AlertCircle, CheckCircle2, Printer, RefreshCw, Save } from 'lucide-react'
import { apiRequest } from '../api/client'
import Modal from './Modal'

export default function PrinterSettingsModal({ onClose }) {
  const [settings, setSettings] = useState(null)
  const [selected, setSelected] = useState('')
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)
  const [previewError, setPreviewError] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const busyRef = useRef(false)

  async function load() {
    setLoading(true)
    setError('')
    try {
      const data = await apiRequest('/api/printing/settings')
      setSettings(data)
      setSelected(data.impresora)
    } catch (e) { setError(e.message) }
    finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  async function perform(test) {
    if (busyRef.current) return
    busyRef.current = true
    setBusy(true)
    setError('')
    setMessage('')
    try {
      if (test) {
        const result = await apiRequest('/api/printing/test', { method: 'POST', body: { solicitudId: crypto.randomUUID() } })
        if (result.estado === 'ENVIADO') setMessage(result.mensaje)
        else setError(result.mensaje)
      } else {
        const data = await apiRequest('/api/printing/settings', { method: 'PUT', body: { impresora: selected } })
        setSettings(data)
        setMessage(selected ? 'Impresora de tickets guardada.' : 'Impresora de tickets desactivada.')
      }
    } catch (e) { setError(test ? 'No se pudo confirmar el envio. Revise la cola de Windows antes de repetir la prueba.' : e.message) }
    finally { busyRef.current = false; setBusy(false) }
  }

  const changed = settings && selected !== settings.impresora
  const available = !changed && settings?.disponible
  const status = loading ? 'Buscando impresoras...' : changed ? 'Cambios sin guardar' : available ? 'Disponible en Windows' : settings?.impresora ? 'No disponible' : 'Sin configurar'

  return <Modal title="Impresora de tickets" size="large" onClose={() => { if (!busyRef.current) onClose() }} footer={<>
    <button type="button" className="secondary-button" disabled={busy || loading || !available} onClick={() => perform(true)}><Printer size={18} />Imprimir prueba</button>
    <button type="button" className="primary-button" disabled={busy || loading || !settings} onClick={() => perform(false)}><Save size={18} />{busy ? 'Procesando...' : 'Guardar configuracion'}</button>
  </>}>
    <div className="printer-settings">
      <section className="printer-settings__controls" aria-label="Configuracion de impresora">
        <header className="printer-settings__device">
          <span className="printer-settings__icon"><Printer size={25} /></span>
          <div><span>Impresora de tickets</span><strong>{selected || 'Seleccionar impresora'}</strong></div>
        </header>
        <div className={'printer-settings__status' + (available ? ' printer-settings__status--ready' : '')} role="status">
          {available ? <CheckCircle2 size={16} /> : <AlertCircle size={16} />}<span>{status}</span>
        </div>
        <div className="form-field"><label htmlFor="ticket-printer">Impresora de Windows</label><div className="printer-settings__select">
          <select id="ticket-printer" value={selected} disabled={busy || loading || !settings} onChange={(e) => { setSelected(e.target.value); setMessage('') }}>
            <option value="">Sin impresora de tickets</option>
            {settings?.impresora && !settings.impresoras.includes(settings.impresora) && <option value={settings.impresora}>{settings.impresora} (no disponible)</option>}
            {settings?.impresoras.map((name) => <option key={name} value={name}>{name}</option>)}
          </select>
          <button type="button" className="icon-button" title="Actualizar impresoras" aria-label="Actualizar impresoras" disabled={busy || loading} onClick={load}><RefreshCw size={19} /></button>
        </div></div>
        <dl className="printer-settings__facts">
          <div><dt>Papel</dt><dd>Rollo de 58 mm</dd></div>
          <div><dt>Documento</dt><dd>Nota de pedido</dd></div>
          <div><dt>Comercio</dt><dd>LaFerreteria</dd></div>
          <div><dt>Domicilio</dt><dd>Domicilio del comercio, Localidad</dd></div>
          <div><dt>Contacto</dt><dd>Configurar</dd></div>
        </dl>
        {settings && !settings.impresoras.length && <div className="ticket-notice" role="status">No hay impresoras de tickets disponibles. Conecte la NexusPOS por USB e instale su controlador en Windows.</div>}
        {error && <div className="sale-error" role="alert">{error}</div>}
        {message && <div className="ticket-notice ticket-notice--success" role="status">{message}</div>}
      </section>
      <section className="printer-settings__preview" aria-label="Vista previa de la nota de pedido">
        <header><h3>Nota de pedido</h3><span>58 mm</span></header>
        <div className="ticket-preview">{previewError ? <p role="alert">No se pudo cargar la vista previa.</p> : <img src="/api/printing/sample" alt="Nota de pedido LaFerreteria de 58 mm con datos de ejemplo del comercio" onError={() => setPreviewError(true)} />}</div>
      </section>
    </div>
  </Modal>
}
