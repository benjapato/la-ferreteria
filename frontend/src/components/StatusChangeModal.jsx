import { Power, PowerOff } from 'lucide-react'
import Modal from './Modal'

export default function StatusChangeModal({ label, active, submitting, error, onClose, onConfirm }) {
  return (
    <Modal title={active ? `Desactivar ${label}` : `Activar ${label}`} description={active ? 'El registro dejara de estar disponible para nuevas operaciones.' : 'El registro volvera a estar disponible.'} onClose={onClose} footer={<><button className="secondary-button" type="button" onClick={onClose} disabled={submitting}>Cancelar</button><button className={active ? 'danger-button' : 'primary-button'} type="button" onClick={onConfirm} disabled={submitting}>{active ? <PowerOff size={18} /> : <Power size={18} />}{submitting ? 'Actualizando...' : active ? 'Desactivar' : 'Activar'}</button></>}>
      <div className={active ? 'status-change status-change--off' : 'status-change'}>{active ? <PowerOff size={25} /> : <Power size={25} />}<div><strong>{active ? 'No se eliminara ningun dato' : 'Se conservaran todos los datos'}</strong><p>{active ? 'Las ventas y movimientos anteriores seguiran disponibles en el historial.' : 'Podras utilizarlo nuevamente en las operaciones correspondientes.'}</p></div></div>
      {error && <p className="form-error" role="alert">{error}</p>}
    </Modal>
  )
}
