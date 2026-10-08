import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { useProfileStatus } from '../profile/ProfileStatusContext'

// Everything behind this needs a logged-in user
export function RequireAuth() {
  const { isAuthenticated } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return <Outlet />
}

// Login and register make no sense for someone who is already in
export function RequireGuest() {
  const { isAuthenticated } = useAuth()
  const location = useLocation()

  if (isAuthenticated) {
    const from = (location.state as { from?: string } | null)?.from
    return <Navigate to={from ?? '/'} replace />
  }
  return <Outlet />
}

// Recommendations, connections and chat stay locked until the profile is filled in
export function RequireProfile() {
  const { status, refresh } = useProfileStatus()

  if (status === 'loading') {
    return <p className="page-status">Loading…</p>
  }
  if (status === 'error') {
    return (
      <div className="page-status">
        <p>Could not reach the server.</p>
        <button type="button" className="button" onClick={refresh}>
          Try again
        </button>
      </div>
    )
  }
  if (status === 'incomplete') {
    return <Navigate to="/profile" replace />
  }
  return <Outlet />
}
