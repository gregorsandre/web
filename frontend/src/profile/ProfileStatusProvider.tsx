import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { ApiError } from '../api/client'
import { getBio } from '../api/users'
import { ProfileStatusContext, type ProfileStatus, type ProfileStatusValue } from './ProfileStatusContext'

export function ProfileStatusProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<ProfileStatus>('loading')
  const [version, setVersion] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    // A bios row only exists once the user has filled in the form, so a 404
    // on /me/bio is how an unfinished profile shows up.
    getBio('me', controller.signal)
      .then(() => setStatus('complete'))
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setStatus(err instanceof ApiError && err.status === 404 ? 'incomplete' : 'error')
      })
    return () => controller.abort()
  }, [version])

  const refresh = useCallback(() => {
    setStatus('loading')
    setVersion((v) => v + 1)
  }, [])

  const value = useMemo<ProfileStatusValue>(() => ({ status, refresh }), [status, refresh])

  return <ProfileStatusContext.Provider value={value}>{children}</ProfileStatusContext.Provider>
}
