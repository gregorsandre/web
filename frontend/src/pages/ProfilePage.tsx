import { useState } from 'react'
import { getAbout, getBio, getUser, nullIfNotFound } from '../api/users'
import { Avatar } from '../components/Avatar'
import { BioDetails } from '../components/BioDetails'
import { LoadError } from '../components/UserList'
import { useAsync } from '../hooks/useAsync'
import { useProfileStatus } from '../profile/ProfileStatusContext'
import { ProfileForm } from './ProfileForm'

export function ProfilePage() {
  const profileStatus = useProfileStatus()
  const [editing, setEditing] = useState(false)

  const state = useAsync('me', async (signal) => {
    // Each part may be missing for a brand-new account
    const [user, about, bio] = await Promise.all([
      nullIfNotFound(getUser('me', signal)),
      nullIfNotFound(getAbout('me', signal)),
      nullIfNotFound(getBio('me', signal)),
    ])
    return { user, about: about ?? '', bio }
  })

  if (state.status === 'loading') {
    return <p className="page-status">Loading…</p>
  }
  if (state.status === 'error') {
    return <LoadError onRetry={state.reload} />
  }

  const { user, about, bio } = state.data
  const name = user?.name ?? ''

  function handleSaved() {
    setEditing(false)
    state.reload()
    // unlocks Discover, Connections and Chats once the bio exists
    profileStatus.refresh()
  }

  if (!bio || editing) {
    return (
      <section className="page">
        <h1>{bio ? 'Edit profile' : 'Set up your profile'}</h1>
        {!bio && <p className="notice">Finish your profile to start seeing recommendations.</p>}
        <ProfileForm
          name={name}
          about={about}
          bio={bio}
          onSaved={handleSaved}
          onCancel={bio ? () => setEditing(false) : undefined}
        />
      </section>
    )
  }

  return (
    <section className="page">
      <header className="profile-head">
        <Avatar src={user?.avatar ?? null} name={name} size="large" />
        <h1>{name || 'Unnamed'}</h1>
        <button type="button" className="button" onClick={() => setEditing(true)}>
          Edit
        </button>
      </header>
      {about && <p className="about">{about}</p>}
      <BioDetails bio={bio} />
    </section>
  )
}
