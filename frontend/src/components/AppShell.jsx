import { useEffect, useState } from 'react'
import { AlertTriangle, Banknote, BarChart3, Boxes, Clock3, DatabaseBackup, History, Home, LogOut, Menu, PackageSearch, PanelLeftClose, ShoppingCart, Truck, UserCog, UsersRound, X } from 'lucide-react'
import { Link, NavLink } from 'react-router-dom'
import { apiRequest } from '../api/client'
import { useAuth } from '../auth/useAuth'
import CashCloseModal from './CashCloseModal'

export default function AppShell({ children, onCashChanged, cashStatusKey }) {
  const { logout, user } = useAuth()
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [loggingOut, setLoggingOut] = useState(false)
  const [cashStatus, setCashStatus] = useState(null)
  const [reconciliationCash, setReconciliationCash] = useState(null)
  const [cashRefresh, setCashRefresh] = useState(0)

  useEffect(() => {
    let active = true

    async function loadCashStatus() {
      try {
        const status = await apiRequest('/api/cash/status')
        if (active) setCashStatus(status)
      } catch {
        // El estado general de la pagina sigue disponible si falla este aviso.
      }
    }

    loadCashStatus()
    const interval = window.setInterval(loadCashStatus, 30000)
    return () => {
      active = false
      window.clearInterval(interval)
    }
  }, [cashRefresh, cashStatusKey])

  async function handleReconciled() {
    setReconciliationCash(null)
    setCashStatus((current) => current ? { ...current, arqueoPendiente: null } : current)
    setCashRefresh((current) => current + 1)
    await onCashChanged?.()
  }

  async function handleLogout() {
    setLoggingOut(true)
    try {
      await logout()
    } finally {
      setLoggingOut(false)
    }
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <button className="icon-button topbar__menu" type="button" aria-label="Abrir menu" onClick={() => setMobileMenuOpen(true)}>
          <Menu size={21} />
        </button>
        <Link className="topbar__brand" to="/" aria-label="Ir al inicio">
          <span className="brand-mark">LF</span>
          <span className="brand-copy"><strong>La Ferreteria</strong><small>Gestion comercial</small></span>
        </Link>
        <div className="topbar__account">
          <span className="account-copy">
            <strong>{user.nombreCompleto}</strong>
            <small>{user.rol === 'ADMINISTRADOR' ? 'Administrador' : 'Vendedor'}</small>
          </span>
          <button className="logout-button" type="button" aria-label="Cerrar sesion" onClick={handleLogout} disabled={loggingOut}>
            <LogOut size={18} />
            <span>{loggingOut ? 'Saliendo...' : 'Cerrar sesion'}</span>
          </button>
        </div>
      </header>

      <aside className={`sidebar ${mobileMenuOpen ? 'sidebar--open' : ''}`}>
        <div className="sidebar__top">
          <span className="sidebar__title">Navegacion</span>
          <PanelLeftClose className="sidebar__hint" size={18} aria-hidden="true" />
          <button className="icon-button sidebar__close" type="button" aria-label="Cerrar menu" onClick={() => setMobileMenuOpen(false)}>
            <X size={20} />
          </button>
        </div>
        <nav aria-label="Navegacion principal">
          <NavLink className={({ isActive }) => `nav-item ${isActive ? 'nav-item--active' : ''}`} to="/" end onClick={() => setMobileMenuOpen(false)}>
            <Home size={20} /><span>Inicio</span>
          </NavLink>
          <NavLink className={({ isActive }) => `nav-item ${isActive ? 'nav-item--active' : ''}`} to="/ventas/nueva" onClick={() => setMobileMenuOpen(false)}>
            <ShoppingCart size={20} /><span>Vender</span>
          </NavLink>
          <NavLink className={({ isActive }) => `nav-item ${isActive ? 'nav-item--active' : ''}`} to="/ventas" end onClick={() => setMobileMenuOpen(false)}>
            <History size={20} /><span>Historial</span>
          </NavLink>
          {user.rol === 'ADMINISTRADOR' && <NavLink className={({ isActive }) => `nav-item ${isActive ? 'nav-item--active' : ''}`} to="/caja" onClick={() => setMobileMenuOpen(false)}>
            <Banknote size={20} /><span>Caja</span>
          </NavLink>}
          <NavLink className={({ isActive }) => `nav-item ${isActive ? 'nav-item--active' : ''}`} to="/productos" onClick={() => setMobileMenuOpen(false)}>
            <Boxes size={20} /><span>Catalogo</span>
          </NavLink>
          <NavLink className={({ isActive }) => `nav-item ${isActive ? 'nav-item--active' : ''}`} to="/clientes" onClick={() => setMobileMenuOpen(false)}>
            <UsersRound size={20} /><span>Clientes</span>
          </NavLink>
          {user.rol === 'ADMINISTRADOR' && <NavLink className={({ isActive }) => `nav-item ${isActive ? 'nav-item--active' : ''}`} to="/compras" onClick={() => setMobileMenuOpen(false)}>
            <Truck size={20} /><span>Proveedores</span>
          </NavLink>}
          {user.rol === 'ADMINISTRADOR' && <NavLink className={({ isActive }) => `nav-item ${isActive ? 'nav-item--active' : ''}`} to="/movimientos-stock" onClick={() => setMobileMenuOpen(false)}>
            <PackageSearch size={20} /><span>Movimientos</span>
          </NavLink>}
          {user.rol === 'ADMINISTRADOR' && <span className="nav-group-label">Analisis</span>}
          {user.rol === 'ADMINISTRADOR' && <NavLink className={({ isActive }) => `nav-item ${isActive ? 'nav-item--active' : ''}`} to="/reportes" onClick={() => setMobileMenuOpen(false)}>
            <BarChart3 size={20} /><span>Reportes</span>
          </NavLink>}
          {user.rol === 'ADMINISTRADOR' && <span className="nav-group-label">Sistema</span>}
          {user.rol === 'ADMINISTRADOR' && <NavLink className={({ isActive }) => `nav-item ${isActive ? 'nav-item--active' : ''}`} to="/usuarios" onClick={() => setMobileMenuOpen(false)}>
            <UserCog size={20} /><span>Usuarios</span>
          </NavLink>}
          {user.rol === 'ADMINISTRADOR' && <NavLink className={({ isActive }) => `nav-item ${isActive ? 'nav-item--active' : ''}`} to="/copias-seguridad" onClick={() => setMobileMenuOpen(false)}>
            <DatabaseBackup size={20} /><span>Copias</span>
          </NavLink>}
        </nav>
        <div className="sidebar__footer">
          <span className="sidebar__status-dot" aria-hidden="true" />
          <span>Sistema conectado</span>
        </div>
      </aside>

      {mobileMenuOpen && <button type="button" className="sidebar-backdrop" aria-label="Cerrar menu" onClick={() => setMobileMenuOpen(false)} />}
      <main className="main-content">
        {cashStatus?.arqueoPendiente && (
          <section className="global-cash-notice global-cash-notice--pending" role="alert">
            <AlertTriangle size={21} />
            <div><strong>Arqueo de caja pendiente</strong><span>La caja #{cashStatus.arqueoPendiente.id} se cerro automaticamente. Debe registrarse el efectivo contado antes de abrir otra.</span></div>
            {user.rol === 'ADMINISTRADOR' && <button className="secondary-button" type="button" onClick={() => setReconciliationCash({ id: cashStatus.arqueoPendiente.id, arqueo: { efectivoEsperado: cashStatus.arqueoPendiente.efectivoEsperado } })}><Banknote size={17} />Regularizar</button>}
          </section>
        )}
        {!cashStatus?.arqueoPendiente && cashStatus?.avisoCierre && (
          <section className="global-cash-notice" role="status">
            <Clock3 size={21} />
            <div><strong>La caja sigue abierta</strong><span>Se cerrara automaticamente a las {cashStatus.horaCierre}. Conta el efectivo y realiza el cierre manual antes de esa hora.</span></div>
          </section>
        )}
        {children}
      </main>
      {reconciliationCash && <CashCloseModal cash={reconciliationCash} mode="reconcile" onClose={() => setReconciliationCash(null)} onClosed={handleReconciled} />}
    </div>
  )
}
