import { api } from './client'

export type ChatSummary = {
  id: number
  // the other person in the chat
  userId: string
  lastMessageAt: string
  unread: number
}

export type Message = {
  id: number
  senderId: string
  body: string
  createdAt: string
}

// What the server pushes over the socket
export type ChatEvent =
  | { type: 'message'; chatId: number; message: Message }
  | { type: 'read'; chatId: number }

export const PAGE_SIZE = 30

export function getChats(signal?: AbortSignal): Promise<ChatSummary[]> {
  return api<ChatSummary[]>('/chats', { signal })
}

export function getChat(chatId: number, signal?: AbortSignal): Promise<ChatSummary> {
  return api<ChatSummary>(`/chats/${chatId}`, { signal })
}

// Returns the id of the one chat with this (connected) user, creating it if needed
export async function openChatWith(userId: string): Promise<number> {
  const { id } = await api<{ id: number }>(`/chats/with/${encodeURIComponent(userId)}`, { method: 'POST' })
  return id
}

// Newest first. Pass the oldest id you have as `before` to get the page before it.
export function getMessages(chatId: number, before?: number, signal?: AbortSignal): Promise<Message[]> {
  const query = new URLSearchParams({ limit: String(PAGE_SIZE) })
  if (before !== undefined) query.set('before', String(before))
  return api<Message[]>(`/chats/${chatId}/messages?${query}`, { signal })
}

export function sendMessage(chatId: number, body: string): Promise<Message> {
  return api<Message>(`/chats/${chatId}/messages`, { method: 'POST', body: { body } })
}

export function markRead(chatId: number): Promise<void> {
  return api<void>(`/chats/${chatId}/read`, { method: 'POST' })
}
