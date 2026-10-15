import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { openChatWith } from '../api/chats'

// On a connected user's profile: opens (or resumes) the one chat with them
export function MessageButton({ userId }: { userId: string }) {
  const navigate = useNavigate()
  const [pending, setPending] = useState(false)
  const [failed, setFailed] = useState(false)

  async function open() {
    setPending(true)
    setFailed(false)
    try {
      navigate(`/chats/${await openChatWith(userId)}`)
    } catch {
      setFailed(true)
      setPending(false)
    }
  }

  return (
    <>
      <button type="button" className="button button-primary" disabled={pending} onClick={open}>
        Message
      </button>
      {failed && <p className="form-error">Could not open the chat. Try again.</p>}
    </>
  )
}
