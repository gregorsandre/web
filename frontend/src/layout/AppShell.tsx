import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { useProfileStatus } from '../profile/ProfileStatusContext'

const NAV_ITEMS = [
  { to: '/recommendations', label: 'Discover', needsProfile: true },
  { to: '/connections', label: 'Connections', needsProfile: true },
  { to: '/chats', label: 'Chats', needsProfile: true },
  { to: '/profile', label: 'Profile', needsProfile: false },
]

export function AppShell() {
  const { logout } = useAuth()
  const { status } = useProfileStatus()
  // TODO: feed this from the chat websocket once chat exists
  const hasUnreadChats = false

  return (
    <div className="shell">
      <header className="shell-header">
        <span className="shell-brand">Sports Buddies</span>
        <button type="button" className="button button-quiet" onClick={logout}>
          Log out
        </button>
      </header>

      <nav className="shell-nav" aria-label="Main">
        {NAV_ITEMS.filter((item) => !item.needsProfile || status === 'complete').map((item) => (
          <NavLink key={item.to} to={item.to} className="shell-nav-link">
            {item.label}
            {item.to === '/chats' && hasUnreadChats && (
              <span className="unread-dot" aria-label="unread messages" />
            )}
          </NavLink>
        ))}
      </nav>

      <main className="shell-main">
        <Outlet />
      </main>
    </div>
  )
}
