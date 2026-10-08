type Props = {
  src: string | null
  name: string
  size?: 'small' | 'large'
}

export function Avatar({ src, name, size = 'small' }: Props) {
  const className = `avatar avatar-${size}`
  if (!src) {
    return (
      <span className={className} role="img" aria-label={`${name} has no picture`}>
        👤
      </span>
    )
  }
  return <img className={className} src={avatarUrl(src)} alt={`${name}'s picture`} />
}

// Pictures served by the backend go through the same /api proxy as everything else
function avatarUrl(src: string): string {
  if (/^https?:\/\//.test(src)) return src
  return `/api${src.startsWith('/') ? '' : '/'}${src}`
}
