import { api } from './client'

// `id` is always the other user. Accept takes the id of whoever sent the request.

export function sendRequest(id: string): Promise<void> {
  return api<void>(`/connections/${encodeURIComponent(id)}`, { method: 'POST' })
}

export function acceptRequest(id: string): Promise<void> {
  return api<void>(`/connections/${encodeURIComponent(id)}`, { method: 'PUT' })
}

export function dismissRecommendation(id: string): Promise<void> {
    return api<void>(`/connections/${encodeURIComponent(id)}`, { method: 'DELETE'})
}

// Rejects an incoming request or ends a connection. The backend also dismisses
// the user, so they are not recommended again.
export function removeConnection(id: string): Promise<void> {
  return api<void>(`/connections/${encodeURIComponent(id)}`, { method: 'DELETE' })
}
