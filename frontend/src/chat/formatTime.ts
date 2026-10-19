// "14:32" for today, "12 Oct 14:32" this year, "12 Oct 2025 14:32" before that
export function formatMessageTime(iso: string, now: Date = new Date()): string {
  const date = new Date(iso)
  const time = date.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit', hourCycle: 'h23' })
  if (date.toDateString() === now.toDateString()) return time
  const day = date.toLocaleDateString(undefined, {
    day: 'numeric',
    month: 'short',
    ...(date.getFullYear() === now.getFullYear() ? {} : { year: 'numeric' }),
  })
  return `${day} ${time}`
}
