import { getBio, getUser, nullIfNotFound, type Bio, type UserSummary } from '../api/users'

export type UserCardData = {
  user: UserSummary
  bio: Bio | null
}

// The API only hands out ids for lists, so each card is assembled from
// /users/{id} and /users/{id}/bio. Users that have become invisible in the
// meantime (404) are left out instead of failing the whole list.
export async function loadUserCards(ids: string[], signal?: AbortSignal): Promise<UserCardData[]> {
  const cards = await Promise.all(
    ids.map(async (id) => {
      const [user, bio] = await Promise.all([
        nullIfNotFound(getUser(id, signal)),
        nullIfNotFound(getBio(id, signal)),
      ])
      return user ? { user, bio } : null
    }),
  )
  return cards.filter((card) => card !== null)
}
