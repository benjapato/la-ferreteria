import { AlertTriangle, Trash2 } from 'lucide-react'
import Modal from './Modal'

export default function BackupDeleteModal({ backup, submitting, error, onClose, onConfirm }) {
  return (
    <Modal title="Eliminar copia" description="Esta accion no se puede deshacer." onClose={onClose} footer={<><button className="secondary-button" type="button" onClick={onClose} disabled={submitting}>Cancelar</button><button className="danger-button" type="button" onClick={onConfirm} disabled={submitting}><Trash2 size={18} />{submitting ? 'Eliminando...' : 'Eliminar copia'}</button></>}>
      <div className="backup-delete-notice"><AlertTriangle size={24} /><div><strong>{backup.nombre}</strong><p>El archivo se borrara definitivamente de la carpeta local de copias.</p></div></div>
      {error && <p className="form-error" role="alert">{error}</p>}
    </Modal>
  )
}
