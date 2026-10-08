import { createContext, useContext } from 'react'

export type ProfileStatus = 'loading' | 'complete' | 'incomplete' | 'error'

export type ProfileStatusValue = {
  status: ProfileStatus
  // call after the profile form is saved, so the gate re-checks
  refresh: () => void
}

export const ProfileStatusContext = createContext<ProfileStatusValue | null>(null)

export function useProfileStatus(): ProfileStatusValue {
  const value = useContext(ProfileStatusContext)
  if (!value) {
    throw new Error('useProfileStatus must be used inside <ProfileStatusProvider>')
  }
  return value
}
