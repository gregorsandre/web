import { useState, type SubmitEvent } from 'react'
import { Link } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'

// Same limits as RegisterRequest on the backend
const PASSWORD_MIN = 8
const PASSWORD_MAX = 72
const EMAIL_MAX = 254

type Mode = 'login' | 'register'

export function LoginPage() {
  return <AuthForm mode="login" />
}

export function RegisterPage() {
  return <AuthForm mode="register" />
}

function AuthForm({ mode }: { mode: Mode }) {
  const auth = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const isRegister = mode === 'register'

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      // On success RequireGuest sees the new session and redirects
      await (isRegister ? auth.register : auth.login)({ email, password })
    } catch (err) {
      setError(errorMessage(mode, err))
      setSubmitting(false)
    }
  }

  return (
    <main className="auth-page">
      <form className="auth-card" onSubmit={handleSubmit}>
        <h1>{isRegister ? 'Create account' : 'Log in'}</h1>

        <label className="field">
          <span>Email</span>
          <input
            type="email"
            autoComplete="email"
            required
            maxLength={EMAIL_MAX}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </label>

        <label className="field">
          <span>Password</span>
          <input
            type="password"
            autoComplete={isRegister ? 'new-password' : 'current-password'}
            required
            minLength={isRegister ? PASSWORD_MIN : undefined}
            maxLength={PASSWORD_MAX}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          {isRegister && <small>At least {PASSWORD_MIN} characters</small>}
        </label>

        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}

        <button type="submit" className="button button-primary" disabled={submitting}>
          {isRegister ? 'Sign up' : 'Log in'}
        </button>

        <p className="auth-switch">
          {isRegister ? (
            <>
              Already have an account? <Link to="/login">Log in</Link>
            </>
          ) : (
            <>
              New here? <Link to="/register">Create an account</Link>
            </>
          )}
        </p>
      </form>
    </main>
  )
}

function errorMessage(mode: Mode, err: unknown): string {
  if (!(err instanceof ApiError)) {
    return 'Could not reach the server. Is the backend running?'
  }
  if (mode === 'register' && err.status === 409) {
    return 'That email is already registered.'
  }
  if (mode === 'login' && err.status === 401) {
    return 'Wrong email or password.'
  }
  if (err.status === 400) {
    return 'Check the email and password and try again.'
  }
  return 'Something went wrong. Please try again.'
}
