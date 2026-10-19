import { useState } from 'react'
import { ApiError } from '../api/client'
import { acceptRequest, removeConnection, sendRequest } from '../api/connections'
import type { Relationship } from '../connections/relationship'

export type ConnectionOutcome = 'accepted' | 'removed'

type Props = {
  userId: string
  relationship: Relationship
  // Called after Accept, Reject or Disconnect succeeded; a sent request is shown in place
  onDone?: (outcome: ConnectionOutcome) => void
}

export function ConnectionActions({ userId, relationship, onDone }: Props) {
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [sent, setSent] = useState(false)

  async function run(action: () => Promise<void>, outcome: ConnectionOutcome | 'sent', confirmText?: string) {
    if (confirmText && !window.confirm(confirmText)) return
    setPending(true)
    setError(null)
    try {
      await action()
      if (outcome === 'sent') setSent(true)
      else onDone?.(outcome)
    } catch (err: unknown) {
      // On Connect, 404 means a request between the two of you already exists
      if (outcome === 'sent' && err instanceof ApiError && err.status === 404) setSent(true)
      else setError('That did not work. Try again.')
    } finally {
      setPending(false)
    }
  }

  if (relationship === 'self') return null
  if (sent) return <p className="action-note">Request sent</p>

  return (
    <div className="connection-actions">
      {relationship === 'none' && (
        <button
          type="button"
          className="button button-primary"
          disabled={pending}
          onClick={() => run(() => sendRequest(userId), 'sent')}
        >
          Connect
        </button>
      )}
      {relationship === 'incoming' && (
        <>
          <button
            type="button"
            className="button button-primary"
            disabled={pending}
            onClick={() => run(() => acceptRequest(userId), 'accepted')}
          >
            Accept
          </button>
          <button
            type="button"
            className="button"
            disabled={pending}
            onClick={() =>
              run(
                () => removeConnection(userId),
                'removed',
                'Reject this request? They will not be recommended to you again.',
              )
            }
          >
            Reject
          </button>
        </>
      )}
      {relationship === 'connected' && (
        <button
          type="button"
          className="button"
          disabled={pending}
          onClick={() =>
            run(() => removeConnection(userId), 'removed', 'Disconnect? They will not be recommended to you again.')
          }
        >
          Disconnect
        </button>
      )}
      {error && <p className="form-error">{error}</p>}
    </div>
  )
}
