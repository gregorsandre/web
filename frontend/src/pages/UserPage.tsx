import { Link, useParams } from 'react-router-dom'
import { getAbout, getBio, getUser, nullIfNotFound } from '../api/users'
import { Avatar } from '../components/Avatar'
import { BioDetails } from '../components/BioDetails'
import { LoadError } from '../components/UserList'
import { useAsync } from '../hooks/useAsync'

export function UserPage() {
  const { id = '' } = useParams()

  const state = useAsync(`user:${id}`, async (signal) => {
    // 404 covers both "no such user" and "not allowed to see them"
    const user = await nullIfNotFound(getUser(id, signal))
    if (!user) return null
    const [about, bio] = await Promise.all([
      nullIfNotFound(getAbout(id, signal)),
      nullIfNotFound(getBio(id, signal)),
    ])
    return { user, about, bio }
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
  return (
    <section className="page">
      <header className="profile-head">
        <Avatar src={user.avatar} name={user.name} size="large" />
        <h1>{user.name || 'Unnamed'}</h1>
      </header>
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
