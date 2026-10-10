import { useEffect, useMemo, useState } from 'react'
import { apiRequest, clearCsrfToken } from '../api/client'
import { AuthContext } from './context'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    apiRequest('/api/auth/me')
      .then((currentUser) => active && setUser(currentUser))
      .catch(() => active && setUser(null))
      .finally(() => active && setLoading(false))
    return () => { active = false }
  }, [])

  const value = useMemo(() => ({
    loading,
    user,
    async login(nombreUsuario, contrasena) {
      const currentUser = await apiRequest('/api/auth/login', {
        method: 'POST',
        body: { nombreUsuario, contrasena },
      })
      setUser(currentUser)
    },
    async logout() {
      try {
        await apiRequest('/api/auth/logout', { method: 'POST' })
      } finally {
        clearCsrfToken()
        setUser(null)
      }
    },
  }), [loading, user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
