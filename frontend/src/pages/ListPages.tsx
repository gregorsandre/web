import { useState } from 'react'
import { getConnectionIds, getRecommendationIds, getRequestIds } from '../api/lists'
import { ConnectionActions, type ConnectionOutcome } from '../components/ConnectionActions'
import { UserList } from '../components/UserList'
import { loadUserCards, type UserCardData } from '../data/userCards'
import { useAsync, type AsyncState } from '../hooks/useAsync'

// The backend caps this too; the slice only guards against a misbehaving server
const MAX_RECOMMENDATIONS = 10

export function RecommendationsPage() {
  const state = useAsync('recommendations', async (signal) => {
    const [ids, connectionIds, requestIds] = await Promise.all([
      getRecommendationIds(signal),
      getConnectionIds(signal),
      getRequestIds(signal),
    ])
    // The backend still recommends people you are connected to or who asked you
    const known = new Set([...connectionIds, ...requestIds])
    // order is kept: strongest match first
    return loadUserCards(ids.filter((id) => !known.has(id)).slice(0, MAX_RECOMMENDATIONS), signal)
  })

  return (
    <section className="page">
      <h1>Discover</h1>
      <UserList
        state={state}
        emptyText="No recommendations right now. Check back later."
        renderActions={(card) => <ConnectionActions userId={card.user.id} relationship="none" />}
      />
    </section>
  )
}

export function ConnectionsPage() {
  const state = useAsync('connections', async (signal) => {
    const [connectionIds, requestIds] = await Promise.all([getConnectionIds(signal), getRequestIds(signal)])
    const [connections, requests] = await Promise.all([
      loadUserCards(connectionIds, signal),
      loadUserCards(requestIds, signal),
    ])
    return { connections, requests }
  })
  // Changes made on this page since the last load, applied on top of it
  const [accepted, setAccepted] = useState<UserCardData[]>([])
  const [removed, setRemoved] = useState<ReadonlySet<string>>(new Set())

  const visible = (cards: UserCardData[]) => cards.filter((card) => !removed.has(card.user.id))
  const requests = mapReady(state, (data) =>
    visible(data.requests).filter((card) => !accepted.some((a) => a.user.id === card.user.id)),
  )
  const connections = mapReady(state, (data) =>
    visible([...accepted.filter((a) => !data.connections.some((c) => c.user.id === a.user.id)), ...data.connections]),
  )

  function handleDone(card: UserCardData, outcome: ConnectionOutcome) {
    if (outcome === 'accepted') setAccepted((list) => [card, ...list])
    else setRemoved((ids) => new Set(ids).add(card.user.id))
  }

  return (
    <section className="page">
      <h1>Connections</h1>
      <h2 className="list-heading">Requests</h2>
      <UserList
        state={requests}
        emptyText="No pending requests."
        renderActions={(card) => (
          <ConnectionActions userId={card.user.id} relationship="incoming" onDone={(o) => handleDone(card, o)} />
        )}
      />
      <h2 className="list-heading">Connected</h2>
      <UserList
        state={connections}
        emptyText="You have no connections yet."
        renderActions={(card) => (
          <ConnectionActions userId={card.user.id} relationship="connected" onDone={(o) => handleDone(card, o)} />
        )}
      />
    </section>
  )
}

// Transforms the data of a loaded state and passes loading/error through
function mapReady<T, U>(
  state: AsyncState<T> & { reload: () => void },
  transform: (data: T) => U,
): AsyncState<U> & { reload: () => void } {
  return state.status === 'ready' ? { status: 'ready', data: transform(state.data), reload: state.reload } : state
}

export function ChatsPage() {
  return (
    <section className="page">
      <h1>Chats</h1>
      <p>Your conversations will show up here, most recent first.</p>
    </section>
  )
}
