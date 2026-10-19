// Single place that knows where the JWT lives. Everything else goes through
// these three functions, so changing the storage strategy only touches this file.

// sessionStorage: the login survives a page refresh but ends when the tab is
// closed, and every tab has its own session, so two users can be logged in
// side by side (handy for trying out chat).
const KEY = 'sportsbuddies.token'

export function getToken(): string | null {
  try {
    return sessionStorage.getItem(KEY)
  } catch {
    // storage can be blocked (private mode, strict browser settings)
    return null
  }
}

export function setToken(token: string): void {
  try {
    sessionStorage.setItem(KEY, token)
  } catch {
    // nothing to do: the session then only lasts until the next refresh
  }
}

export function clearToken(): void {
  try {
    sessionStorage.removeItem(KEY)
  } catch {
    // see setToken
  }
}
