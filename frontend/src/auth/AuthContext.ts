import { createContext, useContext } from 'react'

/** ログインの状態（確認中・ログイン済み・未ログイン） */
export type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated'

/** 画面の部品から使うログインの状態と操作 */
export type AuthContextValue = {
  status: AuthStatus
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

/** ログインの状態を画面の部品に配る入れ物 */
export const AuthContext = createContext<AuthContextValue | null>(null)

/** ログインの状態と操作を取り出す（AuthProviderの内側でだけ使える） */
export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)
  if (value === null) {
    throw new Error('useAuthはAuthProviderの内側で使ってください。')
  }
  return value
}
