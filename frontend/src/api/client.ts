import { getToken } from '../auth/tokenStorage'

const BASE_URL = '/api'

export class ApiError extends Error {
  readonly status: number

  constructor(status: number) {
    super(`Request failed with status ${status}`)
    this.name = 'ApiError'
    this.status = status
  }
}

type RequestOptions = {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: unknown
  // false for the login/register calls, which are made without a token
  auth?: boolean
  signal?: AbortSignal
}

let onSessionExpired: () => void = () => {}

// AuthProvider registers its logout here, so a rejected token anywhere in the
// app drops the user back to the login page.
export function setSessionExpiredHandler(handler: () => void): void {
  onSessionExpired = handler
}

export async function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, auth = true, signal } = options

  const headers: Record<string, string> = {}
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  const token = auth ? getToken() : null
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const res = await fetch(BASE_URL + path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    signal,
  })

  if (!res.ok) {
    // A 401 on an authenticated call means the token is missing, expired or invalid
    if (auth && res.status === 401) {
      onSessionExpired()
    }
    throw new ApiError(res.status)
  }

  const text = await res.text()
  if (!text) {
    return undefined as T
  }
  // /users/{id}/profile answers with a bare string rather than JSON
  const contentType = res.headers.get('Content-Type') ?? ''
  return (contentType.includes('application/json') ? JSON.parse(text) : text) as T
}
