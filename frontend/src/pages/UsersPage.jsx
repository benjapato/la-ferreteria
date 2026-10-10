import { useCallback, useEffect, useState } from 'react'
import {
  AlertTriangle, KeyRound, Pencil, Power, PowerOff, RotateCcw,
  Search, ShieldCheck, UserCheck, UserPlus, UsersRound,
} from 'lucide-react'
import { apiRequest } from '../api/client'
import AppShell from '../components/AppShell'
import StatusChangeModal from '../components/StatusChangeModal'
import UserFormModal from '../components/UserFormModal'

const dateFormatter = new Intl.DateTimeFormat('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric' })

export default function UsersPage() {
  const [data, setData] = useState(null)
  const [search, setSearch] = useState('')
  const [appliedSearch, setAppliedSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [formUser, setFormUser] = useState(undefined)
  const [statusUser, setStatusUser] = useState(null)
  const [submittingStatus, setSubmittingStatus] = useState(false)
  const [modalError, setModalError] = useState('')

  const loadUsers = useCallback(async (term = '') => {
    setLoading(true)
    setError('')
    try {
      const query = new URLSearchParams({ search: term })
      setData(await apiRequest(`/api/users?${query}`))
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadUsers('') }, [loadUsers])

  function applySearch(event) {
    event.preventDefault()
    const term = search.trim()
    setAppliedSearch(term)
    loadUsers(term)
  }

  function clearSearch() {
    setSearch('')
    setAppliedSearch('')
    loadUsers('')
  }

  async function changeStatus() {
    setSubmittingStatus(true)
    setModalError('')
    try {
      await apiRequest(`/api/users/${statusUser.id}/toggle`, { method: 'POST' })
      setStatusUser(null)
      await loadUsers(appliedSearch)
    } catch (requestError) {
      setModalError(requestError.message)
    } finally {
      setSubmittingStatus(false)
    }
  }

  function userSaved() {
    setFormUser(undefined)
    loadUsers(appliedSearch)
  }

  return (
    <AppShell>
      <div className="users-page">
        <div className="page-heading">
          <div><p className="eyebrow">Accesos del sistema</p><h1>Usuarios</h1><p>Administra las personas que pueden ingresar y sus permisos.</p></div>
          <button className="primary-button" type="button" onClick={() => setFormUser(null)}><UserPlus size={18} /><span>Nuevo usuario</span></button>
        </div>

        <div className="user-metrics">
          <UserMetric icon={<UsersRound size={20} />} label="Usuarios" value={data?.total || 0} tone="blue" />
          <UserMetric icon={<UserCheck size={20} />} label="Activos" value={data?.activos || 0} tone="green" />
          <UserMetric icon={<PowerOff size={20} />} label="Inactivos" value={data?.inactivos || 0} tone="red" />
          <UserMetric icon={<ShieldCheck size={20} />} label="Administradores" value={data?.administradores || 0} tone="amber" />
        </div>

        <form className="user-search" onSubmit={applySearch}>
          <div className="filter-input"><Search size={17} /><input aria-label="Buscar usuario" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Nombre completo o usuario" /></div>
          <button className="primary-button" type="submit"><Search size={17} />Buscar</button>
          <button className="icon-button reset-filters" type="button" title="Limpiar busqueda" aria-label="Limpiar busqueda" onClick={clearSearch}><RotateCcw size={18} /></button>
        </form>

        {error && <div className="sale-error" role="alert"><AlertTriangle size={19} /><span>{error}</span></div>}

        <section className="users-panel">
          <header className="history-panel__header"><div><h2>Usuarios registrados</h2><span>{data?.usuarios.length || 0} resultados{appliedSearch ? ' con la busqueda actual' : ''}</span></div></header>
          {loading ? <div className="history-loading" role="status"><div className="skeleton" /><div className="skeleton" /><div className="skeleton" /></div> : data?.usuarios.length ? <div className="table-scroll"><table className="users-table"><thead><tr><th>Persona</th><th>Usuario</th><th>Rol</th><th>Estado</th><th>Creado</th><th className="align-right">Acciones</th></tr></thead><tbody>{data.usuarios.map((user) => <tr key={user.id}><td><div className="user-identity"><span>{initials(user.nombreCompleto)}</span><div><strong>{user.nombreCompleto}</strong><small>ID #{user.id}</small></div></div></td><td><span className="username-label"><KeyRound size={13} />{user.nombreUsuario}</span></td><td><span className={`user-role user-role--${user.rol.toLowerCase()}`}>{user.rol === 'ADMINISTRADOR' ? 'Administrador' : 'Vendedor'}</span></td><td><span className={user.activo ? 'catalog-status catalog-status--active' : 'catalog-status catalog-status--inactive'}>{user.activo ? 'Activo' : 'Inactivo'}</span></td><td>{dateFormatter.format(new Date(user.fechaCreacion))}</td><td><div className="user-actions"><button className="icon-button" type="button" title="Editar usuario" aria-label={`Editar ${user.nombreCompleto}`} onClick={() => setFormUser(user)}><Pencil size={17} /></button><button className={user.activo ? 'icon-button user-action--off' : 'icon-button user-action--on'} type="button" title={user.activo ? 'Desactivar usuario' : 'Activar usuario'} aria-label={`${user.activo ? 'Desactivar' : 'Activar'} ${user.nombreCompleto}`} onClick={() => { setStatusUser(user); setModalError('') }}>{user.activo ? <PowerOff size={17} /> : <Power size={17} />}</button></div></td></tr>)}</tbody></table></div> : <div className="history-empty"><UsersRound size={34} /><strong>{appliedSearch ? 'No hay usuarios que coincidan' : 'Todavia no hay usuarios'}</strong><span>{appliedSearch ? 'Prueba con otro nombre o limpia la busqueda.' : 'Crea un usuario para comenzar.'}</span></div>}
        </section>
      </div>

      {formUser !== undefined && <UserFormModal user={formUser} onClose={() => setFormUser(undefined)} onSaved={userSaved} />}
      {statusUser && <StatusChangeModal label={statusUser.nombreCompleto} active={statusUser.activo} submitting={submittingStatus} error={modalError} onClose={() => { setStatusUser(null); setModalError('') }} onConfirm={changeStatus} />}
    </AppShell>
  )
}

function initials(name) {
  return String(name).split(/\s+/).slice(0, 2).map((word) => word[0]).join('').toUpperCase()
}

function UserMetric({ icon, label, value, tone }) {
  return <div className={`user-metric user-metric--${tone}`}><span>{icon}</span><div><small>{label}</small><strong>{value}</strong></div></div>
}
