import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getConnectionIds, getRequestIds } from '../api/lists'
import { getAbout, getBio, getUser, nullIfNotFound } from '../api/users'
import { Avatar } from '../components/Avatar'
import { BioDetails } from '../components/BioDetails'
import { ConnectionActions, type ConnectionOutcome } from '../components/ConnectionActions'
import { LoadError } from '../components/UserList'
import { relationshipWith } from '../connections/relationship'
import { useAsync } from '../hooks/useAsync'

export function UserPage() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  // Set after Accept, so the page shows Disconnect without reloading
  const [acceptedId, setAcceptedId] = useState<string | null>(null)

  const state = useAsync(`user:${id}`, async (signal) => {
    // 404 covers both "no such user" and "not allowed to see them"
    const user = await nullIfNotFound(getUser(id, signal))
    if (!user) return null
    const [about, bio, me, connectionIds, requestIds] = await Promise.all([
      nullIfNotFound(getAbout(id, signal)),
      nullIfNotFound(getBio(id, signal)),
      getUser('me', signal),
      getConnectionIds(signal),
      getRequestIds(signal),
    ])
    return { user, about, bio, relationship: relationshipWith(user.id, me.id, connectionIds, requestIds) }
  })

  if (state.status === 'loading') {
    return <p className="page-status">Loading…</p>
  }
  if (state.status === 'error') {
    return <LoadError onRetry={state.reload} />
  }
  if (!state.data) {
    return <NotFoundPage />
  }

  const { user, about, bio } = state.data
  const relationship = acceptedId === user.id ? 'connected' : state.data.relationship

  function handleDone(outcome: ConnectionOutcome) {
    // After Reject or Disconnect the profile is no longer visible to you
    if (outcome === 'removed') navigate('/connections', { replace: true })
    else setAcceptedId(user.id)
  }
  return (
    <section className="page">
      <header className="profile-head">
        <Avatar src={user.avatar} name={user.name} size="large" />
        <h1>{user.name || 'Unnamed'}</h1>
      </header>
      <ConnectionActions key={user.id} userId={user.id} relationship={relationship} onDone={handleDone} />
      {about && <p className="about">{about}</p>}
      {bio && <BioDetails bio={bio} />}
    </section>
  )
}

export function NotFoundPage() {
  return (
    <section className="page">
      <h1>Page not found</h1>
      <p>
        <Link to="/">Back to the start</Link>
      </p>
    </section>
  )
}
