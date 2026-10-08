import { api } from './client'
import type { BioInput } from './users'

// NOTE: the backend has no write endpoints for the profile yet. These two are
// the contract this form assumes; adjust here once the real ones exist.

export type ProfileInput = {
  displayName: string
  aboutMe: string
}

export function saveProfile(input: ProfileInput): Promise<void> {
  return api<void>('/me/profile', { method: 'PUT', body: input })
}

export function saveBio(input: BioInput): Promise<void> {
  return api<void>('/me/bio', { method: 'PUT', body: input })
}
