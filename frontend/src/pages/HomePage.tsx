import { useState } from 'react'
import { useAuth } from '../auth/AuthContext'

/** ログイン後の画面（園の選択などは実装中） */
export function HomePage() {
  const { logout } = useAuth()
  const [loggingOut, setLoggingOut] = useState(false)

  async function handleLogout() {
    setLoggingOut(true)
    await logout()
    // 画面の状態を残さないようページを読み込み直してログイン画面へ移る
    window.location.assign('/login')
  }

  return (
    <main className="home">
      <h1>HoikuLink</h1>
      <p>ログインしました。</p>
      <p className="note">園の選択と連絡帳の画面は実装中です。</p>
      <button type="button" onClick={handleLogout} disabled={loggingOut}>
        ログアウト
      </button>
    </main>
  )
}
