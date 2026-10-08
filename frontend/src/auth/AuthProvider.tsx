import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import * as authApi from '../api/auth'
import type { Credentials } from '../api/auth'
import { setSessionExpiredHandler } from '../api/client'
import { AuthContext, type AuthContextValue } from './AuthContext'
import { clearToken, getToken, setToken } from './tokenStorage'

export function AuthProvider({ children }: { children: ReactNode }) {
  // tokenStorage is the source of truth; this state only exists to re-render
  const [token, setTokenState] = useState<string | null>(() => getToken())

  const startSession = useCallback((newToken: string) => {
    setToken(newToken)
    setTokenState(newToken)
  }, [])

  const logout = useCallback(() => {
    clearToken()
    setTokenState(null)
  }, [])

  const login = useCallback(
    async (credentials: Credentials) => {
      const { token: newToken } = await authApi.login(credentials)
      startSession(newToken)
    },
    [startSession],
  )

  const register = useCallback(
    async (credentials: Credentials) => {
      const { token: newToken } = await authApi.register(credentials)
      startSession(newToken)
    },
    [startSession],
  )

  useEffect(() => {
    setSessionExpiredHandler(logout)
    return () => setSessionExpiredHandler(() => {})
  }, [logout])

  const value = useMemo<AuthContextValue>(
    () => ({ isAuthenticated: token !== null, login, register, logout }),
    [token, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
