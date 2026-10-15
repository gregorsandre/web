import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import type { UserCardData } from '../data/userCards'
import type { AsyncState } from '../hooks/useAsync'
import { Avatar } from './Avatar'
import { BioTags } from './BioDetails'

type Props = {
  state: AsyncState<UserCardData[]> & { reload: () => void }
  emptyText: string
  // Buttons can't live inside the card's link, so they get their own row under it
  renderActions?: (card: UserCardData) => ReactNode
}

export function UserList({ state, emptyText, renderActions }: Props) {
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
      {state.data.map((card) => (
        <li key={card.user.id} className={renderActions ? 'user-card-item' : undefined}>
          <Link to={`/users/${card.user.id}`} className="user-card">
            <Avatar src={card.user.avatar} name={card.user.name} />
            <span className="user-card-body">
              <strong>{card.user.name || 'Unnamed'}</strong>
              {card.bio && <BioTags bio={card.bio} />}
            </span>
          </Link>
          {renderActions && <div className="user-card-actions">{renderActions(card)}</div>}
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
