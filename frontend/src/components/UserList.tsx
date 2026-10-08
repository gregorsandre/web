import { Link } from 'react-router-dom'
import type { UserCardData } from '../data/userCards'
import type { AsyncState } from '../hooks/useAsync'
import { Avatar } from './Avatar'
import { BioTags } from './BioDetails'

type Props = {
  state: AsyncState<UserCardData[]> & { reload: () => void }
  emptyText: string
}

export function UserList({ state, emptyText }: Props) {
  if (state.status === 'loading') {
    return <p className="page-status">Loading…</p>
  }
  if (state.status === 'error') {
    return <LoadError onRetry={state.reload} />
  }
  if (state.data.length === 0) {
    return <p className="page-status">{emptyText}</p>
  }
  return (
    <ul className="user-list">
      {state.data.map(({ user, bio }) => (
        <li key={user.id}>
          <Link to={`/users/${user.id}`} className="user-card">
            <Avatar src={user.avatar} name={user.name} />
            <span className="user-card-body">
              <strong>{user.name || 'Unnamed'}</strong>
              {bio && <BioTags bio={bio} />}
            </span>
          </Link>
        </li>
      ))}
    </ul>
  )
}

export function LoadError({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="page-status">
      <p>Could not load this. Is the backend running?</p>
      <button type="button" className="button" onClick={onRetry}>
        Try again
      </button>
    </div>
  )
}
