import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { useAuth } from './auth/useAuth'
import DashboardPage from './pages/DashboardPage'
import CustomerDetailPage from './pages/CustomerDetailPage'
import CustomersPage from './pages/CustomersPage'
import LoginPage from './pages/LoginPage'
import ProductsPage from './pages/ProductsPage'
import PurchasesPage from './pages/PurchasesPage'
import NewPurchasePage from './pages/NewPurchasePage'
import SalesPage from './pages/SalesPage'
import SalesHistoryPage from './pages/SalesHistoryPage'
import ReportsPage from './pages/ReportsPage'
import UsersPage from './pages/UsersPage'
import BackupsPage from './pages/BackupsPage'
import CashPage from './pages/CashPage'
import StockMovementsPage from './pages/StockMovementsPage'
import './App.css'
import './styles/sales-review.css'
import './styles/ticket-printing.css'

const routerBasename = import.meta.env.BASE_URL === '/'
  ? undefined
  : import.meta.env.BASE_URL.replace(/\/$/, '')

function LoadingScreen({ message }) {
  return (
    <div className="app-loading" role="status">
      <span className="app-loading__mark">LF</span>
      <span>{message}</span>
    </div>
  )
}

function ProtectedRoute({ children }) {
  const { loading, user } = useAuth()
  const location = useLocation()
  if (loading) return <LoadingScreen message="Preparando el sistema..." />
  return user ? children : <Navigate to="/login" replace state={{ from: location.pathname }} />
}

function LoginRoute() {
  const { loading, user } = useAuth()
  const location = useLocation()
  if (loading) return <LoadingScreen message="Comprobando la sesion..." />
  return user ? <Navigate to={location.state?.from || '/'} replace /> : <LoginPage />
}

function AdminRoute({ children }) {
  const { user } = useAuth()
  return user?.rol === 'ADMINISTRADOR' ? children : <Navigate to="/" replace />
}

export default function App() {
  return (
    <BrowserRouter basename={routerBasename}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginRoute />} />
          <Route path="/" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
          <Route path="/ventas/nueva" element={<ProtectedRoute><SalesPage /></ProtectedRoute>} />
          <Route path="/ventas" element={<ProtectedRoute><SalesHistoryPage /></ProtectedRoute>} />
          <Route path="/productos" element={<ProtectedRoute><ProductsPage /></ProtectedRoute>} />
          <Route path="/clientes" element={<ProtectedRoute><CustomersPage /></ProtectedRoute>} />
          <Route path="/clientes/:id" element={<ProtectedRoute><CustomerDetailPage /></ProtectedRoute>} />
          <Route path="/compras" element={<ProtectedRoute><AdminRoute><PurchasesPage /></AdminRoute></ProtectedRoute>} />
          <Route path="/compras/nueva" element={<ProtectedRoute><AdminRoute><NewPurchasePage /></AdminRoute></ProtectedRoute>} />
          <Route path="/reportes" element={<ProtectedRoute><AdminRoute><ReportsPage /></AdminRoute></ProtectedRoute>} />
          <Route path="/usuarios" element={<ProtectedRoute><AdminRoute><UsersPage /></AdminRoute></ProtectedRoute>} />
          <Route path="/copias-seguridad" element={<ProtectedRoute><AdminRoute><BackupsPage /></AdminRoute></ProtectedRoute>} />
          <Route path="/caja" element={<ProtectedRoute><AdminRoute><CashPage /></AdminRoute></ProtectedRoute>} />
          <Route path="/movimientos-stock" element={<ProtectedRoute><AdminRoute><StockMovementsPage /></AdminRoute></ProtectedRoute>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
