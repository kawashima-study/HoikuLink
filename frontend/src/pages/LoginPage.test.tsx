import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/client'
import { AuthContext, type AuthContextValue } from '../auth/AuthContext'
import { LoginPage } from './LoginPage'

/** 指定したログインの操作でログイン画面を表示する */
function renderLoginPage(login: AuthContextValue['login']) {
  const value: AuthContextValue = { status: 'unauthenticated', login, logout: vi.fn() }
  render(
    <AuthContext.Provider value={value}>
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('ログイン画面', () => {
  it('未入力のまま送信すると入力を求める文を表示する。', async () => {
    const login = vi.fn<AuthContextValue['login']>()
    renderLoginPage(login)

    await userEvent.click(screen.getByRole('button', { name: 'ログイン' }))

    expect(screen.getByRole('alert')).toHaveTextContent('メールアドレスとパスワードを入力してください。')
    expect(login).not.toHaveBeenCalled()
  })

  it('入力したメールアドレスとパスワードでログインする。', async () => {
    const login = vi.fn<AuthContextValue['login']>().mockResolvedValue(undefined)
    renderLoginPage(login)

    await userEvent.type(screen.getByLabelText('メールアドレス'), 'hanako@example.com')
    await userEvent.type(screen.getByLabelText('パスワード'), 'password')
    await userEvent.click(screen.getByRole('button', { name: 'ログイン' }))

    expect(login).toHaveBeenCalledWith('hanako@example.com', 'password')
  })

  it('ログインに失敗するとAPIのtitleとdetailを表示する。', async () => {
    const error = new ApiError(401, {
      title: 'メールアドレスまたはパスワードが正しくありません。',
      detail: '入力内容を確認してもう一度お試しください。',
      code: 'INVALID_CREDENTIALS',
    })
    renderLoginPage(vi.fn<AuthContextValue['login']>().mockRejectedValue(error))

    await userEvent.type(screen.getByLabelText('メールアドレス'), 'hanako@example.com')
    await userEvent.type(screen.getByLabelText('パスワード'), 'wrong')
    await userEvent.click(screen.getByRole('button', { name: 'ログイン' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'メールアドレスまたはパスワードが正しくありません。入力内容を確認してもう一度お試しください。',
    )
  })
})
