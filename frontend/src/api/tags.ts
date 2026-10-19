import { api } from './client'

export type Tag = { id: number; category: string; name: string }

export function getAllTags(signal?: AbortSignal): Promise<Tag[]> {
  return api<Tag[]>('/tags', { signal })
}

export function getMyTags(signal?: AbortSignal): Promise<Tag[]> {
  return api<Tag[]>('/me/tags', { signal })
}