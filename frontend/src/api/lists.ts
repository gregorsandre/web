import { api } from './client'

// Both endpoints answer with ids only; names, pictures and bios are fetched
// per user afterwards (see data/userCards.ts).

export async function getRecommendationIds(signal?: AbortSignal): Promise<string[]> {
  return toIds(await api<unknown>('/recommendations', { signal }))
}

export async function getConnectionIds(signal?: AbortSignal): Promise<string[]> {
  return toIds(await api<unknown>('/connections', { signal }))
}

// Accepts ["<id>", ...] as well as [{ "id": "<id>" }, ...]
function toIds(raw: unknown): string[] {
  if (!Array.isArray(raw)) return []
  return raw
    .map((item: unknown) => (typeof item === 'string' ? item : (item as { id?: unknown } | null)?.id))
    .filter((id): id is string => typeof id === 'string')
}
