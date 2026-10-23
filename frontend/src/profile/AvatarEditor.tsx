import { useRef, useState, type ChangeEvent } from 'react'
import { removeAvatar, uploadAvatar } from '../api/avatar'
import { squareImage } from './squareImage'

type Props = {
  hasPicture: boolean
  // Called with the new picture link, or null once the picture is removed
  onChange: (avatar: string | null) => void
}

export function AvatarEditor({ hasPicture, onChange }: Props) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [busy, setBusy] = useState<'uploading' | 'removing' | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function run(kind: 'uploading' | 'removing', action: () => Promise<string | null>) {
    setBusy(kind)
    setError(null)
    try {
      onChange(await action())
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong. Please try again.')
    } finally {
      setBusy(null)
    }
  }

  function handleFile(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    // cleared, so choosing the same file again still counts as a change
    e.target.value = ''
    if (!file) return
    run('uploading', async () => (await uploadAvatar(await squareImage(file))).avatar)
  }

  function handleRemove() {
    if (!window.confirm('Remove your picture?')) return
    run('removing', async () => {
      await removeAvatar()
      return null
    })
  }

  return (
    <div className="avatar-editor">
      <div className="avatar-editor-buttons">
        <button type="button" className="button" disabled={busy !== null} onClick={() => inputRef.current?.click()}>
          {busy === 'uploading' ? 'Uploading…' : hasPicture ? 'Change picture' : 'Add picture'}
        </button>
        {hasPicture && (
          <button type="button" className="button button-quiet" disabled={busy !== null} onClick={handleRemove}>
            {busy === 'removing' ? 'Removing…' : 'Remove'}
          </button>
        )}
      </div>
      <input
        ref={inputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        hidden
        onChange={handleFile}
      />
      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}
    </div>
  )
}
