import { usePresence } from './usePresence'

// A dot plus the word, so it doesn't rely on colour; `compact` keeps the word for screen readers only
export function OnlineStatus({ userId, compact = false }: { userId: string; compact?: boolean }) {
  const state = usePresence(userId)
  if (state === 'unknown') return null
  const label = state === 'online' ? 'Online' : 'Offline'
  return (
    <span className={`online-status online-status-${state}`} title={label}>
      <span className="online-dot" aria-hidden="true" />
      <span className={compact ? 'visually-hidden' : undefined}>{label}</span>
    </span>
  )
}
