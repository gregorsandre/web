import { useState } from 'react'
import { getAccount } from '../api/account'
import { getAbout, getBio, getUser, nullIfNotFound } from '../api/users'
import { Avatar } from '../components/Avatar'
import { BioDetails } from '../components/BioDetails'
import { LoadError } from '../components/UserList'
import { useAsync } from '../hooks/useAsync'
import { useProfileStatus } from '../profile/ProfileStatusContext'
import { AvatarEditor } from '../profile/AvatarEditor'
import { ProfileForm } from './ProfileForm'

export function ProfilePage() {
  const profileStatus = useProfileStatus()
  const [editing, setEditing] = useState(false)
  // The picture is saved on its own, so it is tracked here instead of reloading the page
  // (a reload would throw away whatever is typed into the form)
  const [avatarOverride, setAvatarOverride] = useState<string | null | undefined>(undefined)

  const state = useAsync('me', async (signal) => {
    // Each part may be missing for a brand-new account
    const [user, about, bio, account] = await Promise.all([
      nullIfNotFound(getUser('me', signal)),
      nullIfNotFound(getAbout('me', signal)),
      nullIfNotFound(getBio('me', signal)),
      getAccount(signal),
    ])
    return { user, about: about ?? '', bio, account }
  })

  if (state.status === 'loading') {
    return <p className="page-status">Loading…</p>
  }
  if (state.status === 'error') {
    return <LoadError onRetry={state.reload} />
  }

  const { user, about, bio, account } = state.data
  const name = user?.name ?? ''
  const avatar = avatarOverride !== undefined ? avatarOverride : (user?.avatar ?? null)
  const avatarEditor = <AvatarEditor hasPicture={avatar !== null} onChange={setAvatarOverride} />

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
        <div className="avatar-setup">
          <Avatar src={avatar} name={name} size="large" />
          {avatarEditor}
        </div>
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
        <Avatar src={avatar} name={name} size="large" />
        <div className="profile-head-name">
          <h1>{name || 'Unnamed'}</h1>
          <span className="profile-email" title="Only you can see this">
            {account.email}
          </span>
        </div>
        <button type="button" className="button" onClick={() => setEditing(true)}>
          Edit
        </button>
      </header>
      {avatarEditor}
      {about && <p className="about">{about}</p>}
      <BioDetails bio={bio} showAgeRange tags={bio.tags} />
    </section>
  )
}
