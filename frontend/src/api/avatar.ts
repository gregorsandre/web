import { api } from './client'
import type { UserSummary } from './users'

// Answers with the updated /me, so the new picture link can be shown at once
export function uploadAvatar(picture: Blob): Promise<UserSummary> {
  const form = new FormData()
  form.append('file', picture, 'avatar.jpg')
  return api<UserSummary>('/me/avatar', { method: 'PUT', body: form })
}

export function removeAvatar(): Promise<void> {
  return api<void>('/me/avatar', { method: 'DELETE' })
}
