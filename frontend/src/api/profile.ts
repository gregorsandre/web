import { api } from './client'
import type { BioInput } from './users'

export type ProfileSetupInput = {
  displayName: string
  aboutMe: string
  bio: BioInput
  tagIds: number[]
}

export function saveProfileSetup(input: ProfileSetupInput): Promise<void> {
  return api<void>('/me/setup', { method: 'PUT', body: input })
}