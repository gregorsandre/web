import { createContext, useContext } from 'react'
import type { ChatEvent, ChatSummary, ClientEvent } from '../api/chats'

export type ChatListener = (event: ChatEvent) => void

export type ChatContextValue = {
  // null until the first load
  chats: ChatSummary[] | null
  myId: string | null
  // increases each time the socket (re)opens
  connection: number
  hasUnread: boolean
  // returns the unsubscribe function
  subscribe: (listener: ChatListener) => () => void
  // the chat page sets this, so messages arriving there don't count as unread
  setActiveChat: (chatId: number | null) => void
  // sends over the socket if it's open; otherwise the event is dropped
  send: (event: ClientEvent) => void
  // starts watching a user's online status; returns the function that stops it
  watchPresence: (userId: string) => () => void
  // userId -> online, for watched users the server has answered for
  presence: ReadonlyMap<string, boolean>
}

export const ChatContext = createContext<ChatContextValue | null>(null)

export function useChat(): ChatContextValue {
  const value = useContext(ChatContext)
  if (!value) {
    throw new Error('useChat must be used inside <ChatProvider>')
  }
  return value
}
