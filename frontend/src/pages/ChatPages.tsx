import { Link } from 'react-router-dom'
import { getUser, nullIfNotFound, type UserSummary } from '../api/users'
import { useChat } from '../chat/ChatContext'
import { formatMessageTime } from '../chat/formatTime'
import { Avatar } from '../components/Avatar'
import { LoadError } from '../components/UserList'
import { useAsync } from '../hooks/useAsync'

export function ChatsPage() {
  const { chats } = useChat()
  // Keyed on the set of people, not the order, so reordering doesn't refetch names
  const userIds = [...new Set((chats ?? []).map((chat) => chat.userId))].sort()
  const users = useAsync(`chat-users:${userIds.join(',')}`, async (signal) => {
    const found = await Promise.all(userIds.map((id) => nullIfNotFound(getUser(id, signal))))
    return new Map(found.filter((user): user is UserSummary => user !== null).map((user) => [user.id, user]))
  })

  if (chats === null) {
    return <p className="page-status">Loading…</p>
  }
  if (users.status === 'error') {
    return <LoadError onRetry={users.reload} />
  }

  return (
    <section className="page">
      <h1>Chats</h1>
      {chats.length === 0 ? (
        <p className="page-status">No chats yet. Open a connection's profile and press Message.</p>
      ) : (
        <ul className="chat-list">
          {chats.map((chat) => {
            const user = users.status === 'ready' ? users.data.get(chat.userId) : undefined
            const name = user?.name || (users.status === 'ready' ? 'Unnamed' : '…')
            return (
              <li key={chat.id}>
                <Link to={`/chats/${chat.id}`} className="chat-row">
                  <Avatar src={user?.avatar ?? null} name={name} />
                  <span className="chat-row-body">
                    <strong>{name}</strong>
                    <time dateTime={chat.lastMessageAt}>{formatMessageTime(chat.lastMessageAt)}</time>
                  </span>
                  {chat.unread > 0 && (
                    <span className="unread-badge" aria-label={`${chat.unread} unread`}>
                      {chat.unread}
                    </span>
                  )}
                </Link>
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}
