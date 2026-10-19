import { useEffect } from 'react'
import { useChat } from './ChatContext'

export type PresenceState = 'online' | 'offline' | 'unknown'

// Watches a user's online status while the calling component is mounted.
// 'unknown' until the server answers, and also when you're not allowed to see it.
export function usePresence(userId: string): PresenceState {
  const { presence, watchPresence } = useChat()
  useEffect(() => watchPresence(userId), [userId, watchPresence])
  const online = presence.get(userId)
  if (online === undefined) return 'unknown'
  return online ? 'online' : 'offline'
}
