import { api, ApiError } from './client'

// Shapes follow the backend records in tech.kood.backend.profilebio

export type UserSummary = {
  id: string
  name: string
  avatar: string | null
}

export const GOALS = ['fun', 'fitness', 'competition', 'event_training'] as const
export const LOOKING_FOR = ['regular_partner', 'casual_games'] as const
export const ENV_PREFS = ['indoor', 'outdoor', 'both'] as const
export const AVAILABILITY = ['weekday_morning', 'weekday_evening', 'weekend'] as const

export type Goal = (typeof GOALS)[number]
export type LookingFor = (typeof LOOKING_FOR)[number]
export type EnvPref = (typeof ENV_PREFS)[number]
export type Availability = (typeof AVAILABILITY)[number]

export type BioInput = {
  skillLevel: number
  goal: Goal
  lookingFor: LookingFor
  envPref: EnvPref
  age: number
  ageMin: number
  ageMax: number
  lat: number
  lng: number
  maxRadiusKm: number
  availability: Availability[]
}

export type Bio = BioInput & {
  userId: string
  updatedAt: string
}

// `id` is a user id or "me"
export function getUser(id: string, signal?: AbortSignal): Promise<UserSummary> {
  return api<UserSummary>(userPath(id), { signal })
}

export async function getAbout(id: string, signal?: AbortSignal): Promise<string> {
  // Currently a bare string; accept an object too, since the brief wants every
  // /users response to carry the id.
  const raw = await api<string | { aboutMe?: string } | undefined>(`${userPath(id)}/profile`, { signal })
  if (typeof raw === 'string') return raw
  return raw?.aboutMe ?? ''
}

export function getBio(id: string, signal?: AbortSignal): Promise<Bio> {
  return api<Bio>(`${userPath(id)}/bio`, { signal })
}

// Turns "this user has no such thing" into null and lets real failures through
export function nullIfNotFound<T>(promise: Promise<T>): Promise<T | null> {
  return promise.catch((err: unknown) => {
    if (err instanceof ApiError && err.status === 404) return null
    throw err
  })
}

function userPath(id: string): string {
  return id === 'me' ? '/me' : `/users/${encodeURIComponent(id)}`
}
