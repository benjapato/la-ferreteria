import { useCallback, useEffect, useState } from 'react'
import {
  AlertTriangle, CalendarClock, DatabaseBackup, Download, FolderOpen,
  HardDrive, Plus, RefreshCw, ShieldCheck, Trash2,
} from 'lucide-react'
import { apiRequest } from '../api/client'
import AppShell from '../components/AppShell'
import BackupDeleteModal from '../components/BackupDeleteModal'

const dateFormatter = new Intl.DateTimeFormat('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit' })

export default function BackupsPage() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [deleteBackup, setDeleteBackup] = useState(null)
  const [deleting, setDeleting] = useState(false)
  const [modalError, setModalError] = useState('')

  const loadBackups = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setData(await apiRequest('/api/backups'))
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadBackups() }, [loadBackups])

  async function createBackup() {
    setCreating(true)
    setError('')
    setMessage('')
    try {
      const backup = await apiRequest('/api/backups', { method: 'POST' })
      setMessage(`Copia creada correctamente: ${backup.nombre}`)
      await loadBackups()
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setCreating(false)
    }
  }

  async function confirmDelete() {
    setDeleting(true)
    setModalError('')
    try {
      await apiRequest(`/api/backups/${encodeURIComponent(deleteBackup.nombre)}`, { method: 'DELETE' })
      setDeleteBackup(null)
      setMessage('La copia se elimino correctamente.')
      await loadBackups()
    } catch (requestError) {
      setModalError(requestError.message)
    } finally {
      setDeleting(false)
    }
  }

  return (
    <AppShell>
      <div className="backups-page">
        <div className="page-heading">
          <div><p className="eyebrow">Proteccion de datos</p><h1>Copias de seguridad</h1><p>Respalda la base PostgreSQL y conserva copias fuera de esta computadora.</p></div>
          <button className="primary-button" type="button" aria-label={creating ? 'Creando copia de seguridad' : 'Crear copia de seguridad'} title="Crear copia de seguridad" onClick={createBackup} disabled={creating}><Plus size={18} /><span>{creating ? 'Creando copia...' : 'Crear copia ahora'}</span></button>
        </div>

        {error && <div className="sale-error" role="alert"><AlertTriangle size={19} /><span>{error}</span></div>}
        {message && <div className="backup-success" role="status"><ShieldCheck size={19} /><span>{message}</span><button className="icon-button" type="button" aria-label="Cerrar mensaje" onClick={() => setMessage('')}>x</button></div>}

        <div className="backup-status-grid">
          <div className="backup-status"><span><CalendarClock size={21} /></span><div><small>Backup diario</small><strong>{data?.automaticasHabilitadas ? 'Activo a las 18:00' : 'Desactivado'}</strong><p>{data?.zona || 'America/Argentina/Buenos_Aires'}</p></div></div>
          <div className="backup-status"><span><DatabaseBackup size={21} /></span><div><small>Copias guardadas</small><strong>{data?.copias.length || 0}</strong><p>No se borran automaticamente.</p></div></div>
          <div className="backup-status"><span><HardDrive size={21} /></span><div><small>Espacio utilizado</small><strong>{formatBytes(data?.tamanioTotalBytes || 0)}</strong><p>Solo archivos .backup</p></div></div>
        </div>

        <div className="backup-location"><FolderOpen size={19} /><div><span>Carpeta local</span><strong title={data?.directorio}>{data?.directorio || 'Cargando ubicacion...'}</strong></div><button className="icon-button" type="button" title="Actualizar lista" aria-label="Actualizar lista" onClick={loadBackups} disabled={loading}><RefreshCw className={loading ? 'spin' : ''} size={18} /></button></div>

        <div className="backup-warning"><AlertTriangle size={21} /><div><strong>Guarda tambien una copia fuera de esta PC</strong><p>Descargala en un pendrive, otra computadora o almacenamiento en la nube. Una copia en el mismo disco no protege ante una falla fisica.</p></div></div>

        <section className="backups-panel">
          <header className="history-panel__header"><div><h2>Archivos disponibles</h2><span>{data?.copias.length || 0} copias locales</span></div></header>
          {loading ? <div className="history-loading" role="status"><div className="skeleton" /><div className="skeleton" /><div className="skeleton" /></div> : data?.copias.length ? <div className="table-scroll"><table className="backups-table"><thead><tr><th>Archivo</th><th>Fecha y hora</th><th className="align-right">Tamano</th><th className="align-right">Acciones</th></tr></thead><tbody>{data.copias.map((backup) => <tr key={backup.nombre}><td><div className="backup-file"><DatabaseBackup size={19} /><div><strong>{backup.nombre}</strong><small>PostgreSQL backup</small></div></div></td><td>{dateFormatter.format(new Date(backup.fechaModificacion))}</td><td className="align-right">{backup.tamanioLegible}</td><td><div className="backup-actions"><a className="icon-button backup-action--download" href={`/api/backups/${encodeURIComponent(backup.nombre)}/download`} title="Descargar copia" aria-label={`Descargar ${backup.nombre}`}><Download size={17} /></a><button className="icon-button backup-action--delete" type="button" title="Eliminar copia" aria-label={`Eliminar ${backup.nombre}`} onClick={() => { setDeleteBackup(backup); setModalError('') }}><Trash2 size={17} /></button></div></td></tr>)}</tbody></table></div> : <div className="history-empty"><DatabaseBackup size={34} /><strong>Todavia no hay copias de seguridad</strong><span>Crea la primera copia para proteger los datos actuales.</span><button className="primary-button" type="button" onClick={createBackup} disabled={creating}><Plus size={17} />Crear copia</button></div>}
        </section>

        <section className="restore-guide"><header><ShieldCheck size={20} /><div><h2>Restaurar una copia</h2><p>La restauracion reemplaza los datos actuales.</p></div></header><ol><li>Detene la aplicacion.</li><li>Abri una terminal de Windows como administrador.</li><li>Ejecuta <code>pg_restore</code> indicando la base <code>laferreteria_react</code> y el archivo descargado.</li><li>Cuando termine, inicia nuevamente la aplicacion.</li></ol></section>
      </div>

      {deleteBackup && <BackupDeleteModal backup={deleteBackup} submitting={deleting} error={modalError} onClose={() => { setDeleteBackup(null); setModalError('') }} onConfirm={confirmDelete} />}
    </AppShell>
  )
}

function formatBytes(bytes) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 ** 2) return `${(bytes / 1024).toFixed(2)} KB`
  return `${(bytes / 1024 ** 2).toFixed(2)} MB`
}
