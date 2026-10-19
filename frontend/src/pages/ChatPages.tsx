import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getChat, getMessages, markRead, PAGE_SIZE, sendMessage, type Message } from '../api/chats'
import { getUser, nullIfNotFound, type UserSummary } from '../api/users'
import { useChat } from '../chat/ChatContext'
import { formatMessageTime } from '../chat/formatTime'
import { OnlineStatus } from '../chat/OnlineStatus'
import { Avatar } from '../components/Avatar'
import { LoadError } from '../components/UserList'
import { useAsync } from '../hooks/useAsync'

// Typing: tell the other person at most every 2 s; show their indicator for 4 s after the last signal
const TYPING_SEND_INTERVAL_MS = 2_000
const TYPING_VISIBLE_MS = 4_000

export function ChatsPage() {
  const { chats } = useChat()
  // Keyed on the set of people, not the order, so reordering doesn't refetch names
  const userIds = [...new Set((chats ?? []).map((chat) => chat.userId))].sort()
  const users = useAsync(`chat-users:${userIds.join(',')}`, async (signal) => {
    const found = await Promise.all(userIds.map((id) => nullIfNotFound(getUser(id, signal))))
    return new Map(found.filter((user): user is UserSummary => user !== null).map((user) => [user.id, user]))
  })

  if (chats === null) {
    return <p className="page-status">Loading…</p>
  }
  if (users.status === 'error') {
    return <LoadError onRetry={users.reload} />
  }

  return (
    <section className="page">
      <h1>Chats</h1>
      {chats.length === 0 ? (
        <p className="page-status">No chats yet. Open a connection's profile and press Message.</p>
      ) : (
        <ul className="chat-list">
          {chats.map((chat) => {
            const user = users.status === 'ready' ? users.data.get(chat.userId) : undefined
            const name = user?.name || (users.status === 'ready' ? 'Unnamed' : '…')
            return (
              <li key={chat.id}>
                <Link to={`/chats/${chat.id}`} className="chat-row">
                  <Avatar src={user?.avatar ?? null} name={name} />
                  <span className="chat-row-body">
                    <strong>
                      {name} <OnlineStatus userId={chat.userId} compact />
                    </strong>
                    <time dateTime={chat.lastMessageAt}>{formatMessageTime(chat.lastMessageAt)}</time>
                  </span>
                  {chat.unread > 0 && (
                    <span className="unread-badge" aria-label={`${chat.unread} unread`}>
                      {chat.unread}
                    </span>
                  )}
                </Link>
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}

// Keyed by the id, so moving to another chat starts with fresh state (pages, draft, errors)
export function ChatPage() {
  const { id = '' } = useParams()
  return <ChatView key={id} id={id} />
}

function ChatView({ id }: { id: string }) {
  const chatId = Number(id)
  const valid = Number.isInteger(chatId) && chatId > 0
  const { myId, connection, subscribe, setActiveChat, send } = useChat()

  const head = useAsync(`chat:${id}`, async (signal) => {
    if (!valid) return null
    // 404 covers "no such chat", "not yours" and "no longer connected"
    const chat = await nullIfNotFound(getChat(chatId, signal))
    if (!chat) return null
    const [user, page] = await Promise.all([
      nullIfNotFound(getUser(chat.userId, signal)),
      getMessages(chatId, undefined, signal),
    ])
    return { chat, user, page }
  })

  // On top of the first page: earlier pages, and messages that arrived while the page is open
  const [older, setOlder] = useState<Message[]>([])
  const [live, setLive] = useState<Message[]>([])
  const [olderPageWasFull, setOlderPageWasFull] = useState<boolean | null>(null)
  const [draft, setDraft] = useState('')
  const [sending, setSending] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const inputRef = useRef<HTMLTextAreaElement>(null)
  const firstConnection = useRef(connection)
  const [otherTyping, setOtherTyping] = useState(false)
  const typingTimerRef = useRef<number | undefined>(undefined)
  const lastTypingSentRef = useRef(0)

  // While this page is open: incoming messages appear here and don't count as unread
  useEffect(() => {
    if (!valid) return
    setActiveChat(chatId)
    markRead(chatId).catch(() => {})
    const unsubscribe = subscribe((event) => {
      if (event.type === 'typing' && event.chatId === chatId && event.userId !== myId) {
        setOtherTyping(true)
        window.clearTimeout(typingTimerRef.current)
        typingTimerRef.current = window.setTimeout(() => setOtherTyping(false), TYPING_VISIBLE_MS)
        return
      }
      if (event.type !== 'message' || event.chatId !== chatId) return
      setLive((list) => [...list, event.message])
      if (event.message.senderId !== myId) {
        // their message is here, so they've stopped typing
        window.clearTimeout(typingTimerRef.current)
        setOtherTyping(false)
        markRead(chatId).catch(() => {})
      }
    })
    return () => {
      unsubscribe()
      window.clearTimeout(typingTimerRef.current)
      // the effect can re-run (e.g. once myId is known); without this a shown indicator would never expire
      setOtherTyping(false)
      setActiveChat(null)
    }
  }, [chatId, valid, myId, subscribe, setActiveChat])

  // After the socket reconnects, fetch the newest page: messages sent during the gap
  useEffect(() => {
    if (!valid || connection === firstConnection.current) return
    getMessages(chatId)
      .then((page) => setLive((list) => [...list, ...page]))
      .catch(() => {})
    markRead(chatId).catch(() => {})
  }, [connection, chatId, valid])

  const messages =
    head.status === 'ready' && head.data ? mergeMessages([...older, ...head.data.page, ...live]) : []

  // Keep the newest message in view; loading earlier pages doesn't change the last id
  const lastId = messages.at(-1)?.id
  useEffect(() => {
    if (lastId !== undefined) window.scrollTo({ top: document.documentElement.scrollHeight })
  }, [lastId])

  if (head.status === 'loading') return <p className="page-status">Loading…</p>
  if (head.status === 'error') return <LoadError onRetry={head.reload} />
  if (!head.data) {
    return (
      <section className="page">
        <h1>This chat isn't available.</h1>
        <p>
          <Link to="/chats">Back to your chats</Link>
        </p>
      </section>
    )
  }

  const { chat, user } = head.data
  const name = user?.name || 'Unnamed'
  const canLoadMore = olderPageWasFull ?? head.data.page.length === PAGE_SIZE

  async function loadEarlier() {
    const oldest = messages[0]?.id
    if (oldest === undefined) return
    setLoadingMore(true)
    try {
      const page = await getMessages(chatId, oldest)
      setOlder((list) => [...list, ...page])
      setOlderPageWasFull(page.length === PAGE_SIZE)
    } catch {
      setError('Could not load earlier messages.')
    } finally {
      setLoadingMore(false)
    }
  }

  async function handleSend(event: FormEvent) {
    event.preventDefault()
    const body = draft.trim()
    if (!body || sending) return
    setSending(true)
    setError(null)
    try {
      const message = await sendMessage(chatId, body)
      setLive((list) => [...list, message])
      setDraft('')
      lastTypingSentRef.current = 0
    } catch {
      setError('Message not sent. Try again.')
    } finally {
      setSending(false)
      // the field stays enabled while sending, so focus is kept; this restores it after a button click
      inputRef.current?.focus()
    }
  }

  function handleDraftChange(value: string) {
    setDraft(value)
    const now = Date.now()
    if (value.trim() && now - lastTypingSentRef.current >= TYPING_SEND_INTERVAL_MS) {
      lastTypingSentRef.current = now
      send({ type: 'typing', chatId })
    }
  }

  // Enter sends and Shift+Enter makes a new line. On touch keyboards Enter stays a new line
  // (there is no Shift), and the Send button sends.
  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key !== 'Enter' || event.shiftKey || event.nativeEvent.isComposing) return
    if (window.matchMedia('(pointer: coarse)').matches) return
    event.preventDefault()
    event.currentTarget.form?.requestSubmit()
  }

  return (
    <section className="page chat-page">
      <header className="chat-head">
        <Link to={`/users/${chat.userId}`} className="chat-head-user">
          <Avatar src={user?.avatar ?? null} name={name} />
          <span className="chat-head-name">
            <strong>{name}</strong>
            <OnlineStatus userId={chat.userId} />
          </span>
        </Link>
      </header>

      <div className="messages">
        {canLoadMore && (
          <button type="button" className="button button-quiet" disabled={loadingMore} onClick={loadEarlier}>
            {loadingMore ? 'Loading…' : 'Load earlier messages'}
          </button>
        )}
        {messages.length === 0 && <p className="page-status">No messages yet. Say hi!</p>}
        <ol className="message-list">
          {messages.map((message) => (
            <li key={message.id} className={message.senderId === myId ? 'message message-own' : 'message'}>
              <p className="message-body">{message.body}</p>
              <time dateTime={message.createdAt}>{formatMessageTime(message.createdAt)}</time>
            </li>
          ))}
        </ol>
      </div>

      <form className="composer" onSubmit={handleSend}>
        <p className="typing-indicator" aria-live="polite">
          {otherTyping ? `${name} is typing…` : ''}
        </p>
        <textarea
          ref={inputRef}
          value={draft}
          onChange={(event) => handleDraftChange(event.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Write a message"
          aria-label="Message"
          rows={2}
          maxLength={2000}
        />
        <button type="submit" className="button button-primary" disabled={sending || !draft.trim()}>
          Send
        </button>
        {error && <p className="form-error">{error}</p>}
      </form>
    </section>
  )
}

// Pages, live events and the POST response overlap (own messages arrive twice):
// keep one per id, oldest first
function mergeMessages(list: Message[]): Message[] {
  const byId = new Map(list.map((message) => [message.id, message]))
  return [...byId.values()].sort((a, b) => a.id - b.id)
}
