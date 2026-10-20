import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import * as client from '../api/client'
import { AuthContext, type AuthStatus } from './AuthContext'

/** ログインの状態を持ち内側の画面の部品に配る */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('loading')

  // 画面を開いたとき（再読み込みを含む）にCookieのリフレッシュトークンでログインを取り戻す
  useEffect(() => {
    let active = true
    client.refreshAccessToken().then((ok) => {
      if (active) {
        setStatus(ok ? 'authenticated' : 'unauthenticated')
      }
    })
    return () => {
      active = false
    }
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    await client.login(email, password)
    setStatus('authenticated')
  }, [])

  const logout = useCallback(async () => {
    await client.logout()
    setStatus('unauthenticated')
  }, [])

  const value = useMemo(() => ({ status, login, logout }), [status, login, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
