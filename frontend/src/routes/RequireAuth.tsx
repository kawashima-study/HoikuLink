import type { ReactNode } from 'react'
import { Navigate } from 'react-router'
import { useAuth } from '../auth/AuthContext'

/** ログイン済みのときだけ中身を表示し未ログインならログイン画面へ移る */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { status } = useAuth()
  if (status === 'loading') {
    return <p className="loading">読み込み中です。</p>
  }
  if (status === 'unauthenticated') {
    return <Navigate to="/login" replace />
  }
  return children
}
