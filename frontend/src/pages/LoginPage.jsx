import { useState } from 'react'
import { Eye, EyeOff, LockKeyhole, LogIn, UserRound } from 'lucide-react'
import { useAuth } from '../auth/useAuth'

export default function LoginPage() {
  const { login } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await login(username.trim(), password)
    } catch (requestError) {
      setError(requestError.status === 401
        ? 'El usuario o la contrasena no son correctos.'
        : 'No pudimos conectar con el sistema. Intenta nuevamente.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="login-page">
      <section className="login-panel" aria-labelledby="login-title">
        <div className="login-brand">
          <span className="login-brand__mark">LF</span>
          <div><strong>La Ferreteria</strong><span>Sistema de gestion</span></div>
        </div>
        <div className="login-content">
          <div className="login-heading">
            <span className="login-heading__icon" aria-hidden="true"><LockKeyhole size={20} /></span>
            <div><h1 id="login-title">Iniciar sesion</h1><p>Ingresa con tu cuenta para continuar.</p></div>
          </div>
          <form onSubmit={handleSubmit}>
            <label className="field-label" htmlFor="username">Usuario</label>
            <div className="input-wrap">
              <UserRound size={18} aria-hidden="true" />
              <input id="username" name="username" autoComplete="username" value={username} onChange={(event) => setUsername(event.target.value)} required autoFocus />
            </div>

            <label className="field-label" htmlFor="password">Contrasena</label>
            <div className="input-wrap">
              <LockKeyhole size={18} aria-hidden="true" />
              <input id="password" name="password" type={showPassword ? 'text' : 'password'} autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} required />
              <button className="password-toggle" type="button" aria-label={showPassword ? 'Ocultar contrasena' : 'Mostrar contrasena'} onClick={() => setShowPassword((visible) => !visible)}>
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>

            {error && <p className="form-error" role="alert">{error}</p>}
            <button className="primary-button login-submit" type="submit" disabled={submitting}>
              <LogIn size={18} /><span>{submitting ? 'Ingresando...' : 'Ingresar'}</span>
            </button>
          </form>
        </div>
      </section>
      <p className="login-footer">Acceso exclusivo para personal autorizado</p>
    </main>
  )
}
