import { useState, type SubmitEvent } from 'react'
import { Navigate, useNavigate } from 'react-router'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'

/** ログイン画面 */
export function LoginPage() {
  const { status, login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [showPassword, setShowPassword] = useState(false)

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (email.trim() === '' || password === '') {
      setError('メールアドレスとパスワードを入力してください。')
      return
    }
    setSubmitting(true) // 送信中はボタンを押せなくし二重のログインを防ぐ
    setError(null)
    try {
      await login(email, password)
      navigate('/', { replace: true })
    } catch (e) {
      setError(toMessage(e))
    } finally {
      setSubmitting(false)
    }
  }

  if (status === 'authenticated') {
    return <Navigate to="/" replace />
  }

  return (
    <main className="login">
      <h1>HoikuLink</h1>
      <form onSubmit={handleSubmit} noValidate>
        <label htmlFor="email">メールアドレス</label>
        <input
          id="email"
          type="email"
          autoComplete="username"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <label htmlFor="password">パスワード</label>
        <input
          id="password"
          type={showPassword ? 'text' : 'password'}
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <button
          type="button"
          className="toggle-password"
          aria-label="パスワードを表示"
          aria-pressed={showPassword}
          onClick={() => setShowPassword((prev) => !prev)}
        >
          {showPassword ? '非表示' : '表示'}
        </button>
        {error !== null && (
          <p className="error" role="alert">
            {error}
          </p>
        )}
        <button type="submit" disabled={submitting}>
          {submitting ? 'ログイン中です。' : 'ログイン'}
        </button>
      </form>
    </main>
  )
}

/** 例外を画面に表示する文に変換する（APIのエラーはtitleとdetailを並べる） */
function toMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.detail === undefined ? error.message : `${error.message}${error.detail}`
  }
  return '通信に失敗しました。時間をおいてもう一度お試しください。'
}