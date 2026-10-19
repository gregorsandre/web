import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { getChats, type ChatEvent, type ChatSummary, type ClientEvent } from '../api/chats'
import { getUser } from '../api/users'
import { getToken } from '../auth/tokenStorage'
import { useProfileStatus } from '../profile/ProfileStatusContext'
import { applyMessage } from './applyMessage'
import { ChatContext, type ChatListener } from './ChatContext'

// Wait before reconnecting: 1 s, 2 s, 5 s, then every 10 s
const RETRY_DELAYS_MS = [1_000, 2_000, 5_000, 10_000]
// Must stay well below the server's 60 s idle timeout (ChatSocketHandler.IDLE_TIMEOUT_MS)
const PING_INTERVAL_MS = 25_000

// One socket per logged-in tab, opened once the profile is complete. Also owns the chat list,
// so the list, the unread dot and the open chat all update from the same events.
export function ChatProvider({ children }: { children: ReactNode }) {
  const { status } = useProfileStatus()
  const enabled = status === 'complete'

  const [chats, setChats] = useState<ChatSummary[] | null>(null)
  const [myId, setMyId] = useState<string | null>(null)
  // Goes up every time the socket (re)opens, so an open chat can fetch what it missed
  const [connection, setConnection] = useState(0)
  // Refs give the socket handlers the current values without reopening the socket
  const chatsRef = useRef<ChatSummary[] | null>(null)
  const myIdRef = useRef<string | null>(null)
  const activeChatRef = useRef<number | null>(null)
  const listenersRef = useRef(new Set<ChatListener>())
  const socketRef = useRef<WebSocket | null>(null)
  // How many mounted components watch each user; the server hears only 0 -> 1 and 1 -> 0
  const watchCountsRef = useRef(new Map<string, number>())
  const [presence, setPresence] = useState<ReadonlyMap<string, boolean>>(new Map())

  const setChatList = useCallback((next: ChatSummary[] | null) => {
    chatsRef.current = next
    setChats(next)
  }, [])

  const reloadChats = useCallback(async () => {
    try {
      const list = await getChats()
      // The open chat is being read right now; a slow reload must not bring its unread count back
      setChatList(list.map((chat) => (chat.id === activeChatRef.current ? { ...chat, unread: 0 } : chat)))
    } catch {
      // a 401 has already logged the user out; anything else keeps the current list
    }
  }, [setChatList])

  useEffect(() => {
    if (!enabled) return

    let socket: WebSocket | null = null
    let retryTimer: number | undefined
    let pingTimer: number | undefined
    let attempt = 0
    let stopped = false

    function scheduleRetry() {
      if (stopped) return
      const delay = RETRY_DELAYS_MS[Math.min(attempt, RETRY_DELAYS_MS.length - 1)]
      attempt++
      retryTimer = window.setTimeout(start, delay)
    }

    function handleEvent(event: ChatEvent) {
      const current = chatsRef.current
      if (event.type === 'message') {
        const next = current && applyMessage(current, event.chatId, event.message, myIdRef.current ?? '', activeChatRef.current)
        if (!next || next === 'unknown') void reloadChats()
        else setChatList(next)
      } else if (event.type === 'read' && current) {
        setChatList(current.map((chat) => (chat.id === event.chatId ? { ...chat, unread: 0 } : chat)))
      }
      if (event.type === 'presence') {
        setPresence((map) => new Map(map).set(event.userId, event.online))
      }
      listenersRef.current.forEach((listener) => listener(event))
    }

    async function start() {
      const token = getToken()
      if (stopped || !token) return
      try {
        if (!myIdRef.current) {
          const me = await getUser('me')
          myIdRef.current = me.id
          setMyId(me.id)
        }
        // An expired token gets its 401 (and the logout) here, before trying the socket
        await reloadChats()
      } catch {
        scheduleRetry()
        return
      }
      if (stopped) return

      const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
      socket = new WebSocket(`${protocol}://${window.location.host}/api/ws?token=${encodeURIComponent(token)}`)
      socketRef.current = socket
      socket.onopen = () => {
        attempt = 0
        // Keep-alive: the server closes sockets silent for 60 s, which is how lost connections go offline
        pingTimer = window.setInterval(() => socket?.send(JSON.stringify({ type: 'ping' })), PING_INTERVAL_MS)
        // Catch up on anything sent between the list load above and the socket opening
        void reloadChats()
        setConnection((n) => n + 1)
        // The server forgets watches with the old socket; tell the new one again
        watchCountsRef.current.forEach((_, userId) => socket?.send(JSON.stringify({ type: 'watch', userId })))
      }
      socket.onmessage = (message) => {
        try {
          handleEvent(JSON.parse(String(message.data)) as ChatEvent)
        } catch {
          // ignore anything that isn't one of our events
        }
      }
      socket.onclose = () => {
        window.clearInterval(pingTimer)
        socket = null
        socketRef.current = null
        // Without a socket the dots would go stale; show nothing until the server answers again
        setPresence(new Map())
        scheduleRetry()
      }
    }

    void start()

    return () => {
      // logout (or an incomplete profile) unmounts/disables the provider: close and stop retrying
      stopped = true
      window.clearTimeout(retryTimer)
      socket?.close()
      socketRef.current = null
      window.clearInterval(pingTimer)
    }
  }, [enabled, reloadChats, setChatList])

  const subscribe = useCallback((listener: ChatListener) => {
    listenersRef.current.add(listener)
    return () => {
      listenersRef.current.delete(listener)
    }
  }, [])

  const setActiveChat = useCallback((chatId: number | null) => {
    activeChatRef.current = chatId
  }, [])

  const send = useCallback((event: ClientEvent) => {
    const socket = socketRef.current
    if (socket && socket.readyState === WebSocket.OPEN) socket.send(JSON.stringify(event))
  }, [])

  const watchPresence = useCallback(
    (userId: string) => {
      const counts = watchCountsRef.current
      const count = (counts.get(userId) ?? 0) + 1
      counts.set(userId, count)
      if (count === 1) send({ type: 'watch', userId })
      return () => {
        const left = (counts.get(userId) ?? 1) - 1
        if (left > 0) {
          counts.set(userId, left)
          return
        }
        counts.delete(userId)
        send({ type: 'unwatch', userId })
      }
    },
    [send],
  )

  const value = useMemo(
    () => ({
      chats,
      myId,
      connection,
      hasUnread: chats?.some((chat) => chat.unread > 0) ?? false,
      subscribe,
      setActiveChat,
      send,
      watchPresence,
      presence,
    }),
    [chats, myId, connection, subscribe, setActiveChat, send, watchPresence, presence],
  )

  return <ChatContext.Provider value={value}>{children}</ChatContext.Provider>
}
