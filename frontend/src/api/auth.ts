import { api } from './client'

export type Credentials = {
  email: string
  password: string
}

type AuthResponse = {
  token: string
}

export function login(credentials: Credentials): Promise<AuthResponse> {
  return api<AuthResponse>('/auth/login', { method: 'POST', body: credentials, auth: false })
}

export function register(credentials: Credentials): Promise<AuthResponse> {
  return api<AuthResponse>('/auth/register', { method: 'POST', body: credentials, auth: false })
}
