let csrfToken = null
let csrfHeader = 'X-CSRF-TOKEN'

export function clearCsrfToken() {
  csrfToken = null
  csrfHeader = 'X-CSRF-TOKEN'
}

export class ApiError extends Error {
  constructor(message, status) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function readResponse(response) {
  if (response.status === 204) return null
  const type = response.headers.get('content-type') || ''
  if (type.includes('application/json')) return response.json()
  const text = await response.text()
  return text ? { mensaje: text } : null
}

async function ensureCsrfToken() {
  if (csrfToken) return

  const response = await fetch('/api/auth/csrf', {
    credentials: 'include',
    headers: { Accept: 'application/json' },
  })
  const data = await readResponse(response)
  if (!response.ok) {
    throw new ApiError('No se pudo iniciar la conexion segura.', response.status)
  }
  csrfToken = data.token
  csrfHeader = data.headerName
}

export async function apiRequest(path, options = {}) {
  const method = (options.method || 'GET').toUpperCase()
  const mutation = !['GET', 'HEAD', 'OPTIONS'].includes(method)
  if (mutation) await ensureCsrfToken()

  const headers = { Accept: 'application/json', ...options.headers }
  if (options.body !== undefined) headers['Content-Type'] = 'application/json'
  if (mutation) headers[csrfHeader] = csrfToken

  const response = await fetch(path, {
    ...options,
    method,
    credentials: 'include',
    headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  })
  const data = await readResponse(response)

  if (!response.ok) {
    if (response.status === 403) clearCsrfToken()
    throw new ApiError(data?.mensaje || 'No se pudo completar la operacion.', response.status)
  }
  return data
}
