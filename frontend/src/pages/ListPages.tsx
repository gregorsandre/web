import { getConnectionIds, getRecommendationIds } from '../api/lists'
import { UserList } from '../components/UserList'
import { loadUserCards } from '../data/userCards'
import { useAsync } from '../hooks/useAsync'

// The backend caps this too; the slice only guards against a misbehaving server
const MAX_RECOMMENDATIONS = 10

export function RecommendationsPage() {
  const state = useAsync('recommendations', async (signal) => {
    const ids = await getRecommendationIds(signal)
    // order is kept: strongest match first
    return loadUserCards(ids.slice(0, MAX_RECOMMENDATIONS), signal)
  })

  return (
    <section className="page">
      <h1>Discover</h1>
      <UserList state={state} emptyText="No recommendations right now. Check back later." />
    </section>
  )
}

export function ConnectionsPage() {
  const state = useAsync('connections', async (signal) =>
    loadUserCards(await getConnectionIds(signal), signal),
  )

  return (
    <section className="page">
      <h1>Connections</h1>
      <UserList state={state} emptyText="You have no connections yet." />
    </section>
  )
}

export function ChatsPage() {
  return (
    <section className="page">
      <h1>Chats</h1>
      <p>Your conversations will show up here, most recent first.</p>
    </section>
  )
}
