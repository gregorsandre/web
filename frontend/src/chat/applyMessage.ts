import type { ChatSummary, Message } from '../api/chats'

// Applies one incoming message to the chat list: that chat moves to the top with the new time,
// and its unread count goes up unless you sent it or you're looking at that chat.
// 'unknown' means the chat isn't in the list yet (first message), so the caller reloads the list.
export function applyMessage(
  chats: ChatSummary[],
  chatId: number,
  message: Message,
  myId: string,
  activeChatId: number | null,
): ChatSummary[] | 'unknown' {
  const chat = chats.find((c) => c.id === chatId)
  if (!chat) return 'unknown'
  const countsAsUnread = message.senderId !== myId && chatId !== activeChatId
  const updated = { ...chat, lastMessageAt: message.createdAt, unread: chat.unread + (countsAsUnread ? 1 : 0) }
  return [updated, ...chats.filter((c) => c.id !== chatId)]
}
