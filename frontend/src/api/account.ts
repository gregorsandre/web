import { api } from './client'

// Login details of the signed-in user; never part of the /me or /users answers
export type Account = {
  email: string
}

export function getAccount(signal?: AbortSignal): Promise<Account> {
  return api<Account>('/account', { signal })
}
