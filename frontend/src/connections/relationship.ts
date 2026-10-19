// How the signed-in user relates to another user; decides which buttons a profile shows.
// 'incoming' means they sent you a request. Requests you sent are not listed by the
// backend, so those users come out as 'none'.
export type Relationship = 'self' | 'connected' | 'incoming' | 'none'

export function relationshipWith(
  userId: string,
  myId: string,
  connectionIds: string[],
  requestIds: string[],
): Relationship {
  if (userId === myId) return 'self'
  // A connection outranks a request, in case both lists ever hold the same user
  if (connectionIds.includes(userId)) return 'connected'
  if (requestIds.includes(userId)) return 'incoming'
  return 'none'
}
