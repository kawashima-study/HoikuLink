import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, hasAccessToken, login, refreshAccessToken, resetForTest } from './client'

/** JSONの本文を持つ応答を作る */
function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

describe('APIの呼び出し', () => {
  const fetchMock = vi.fn<typeof fetch>()

  beforeEach(() => {
    resetForTest()
    vi.stubGlobal('fetch', fetchMock) // ブラウザのfetchを仮の関数に差し替える
  })

  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('ログインに成功するとアクセストークンをメモリに持つ。', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, { accessToken: 'token', tokenType: 'Bearer', expiresIn: 300 }))

    await login('hanako@example.com', 'password')

    expect(hasAccessToken()).toBe(true)
  })

  it('ログインに失敗するとエラーコードを持つ例外になる。', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(401, { title: 'メールアドレスまたはパスワードが正しくありません。', code: 'INVALID_CREDENTIALS' }),
    )

    const error = await login('hanako@example.com', 'wrong').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).code).toBe('INVALID_CREDENTIALS')
    expect(hasAccessToken()).toBe(false)
  })

  it('再発行は独自の見出しを付けて呼び出す。', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, { accessToken: 'token', tokenType: 'Bearer', expiresIn: 300 }))

    await refreshAccessToken()

    const init = fetchMock.mock.calls[0][1]
    expect(init?.headers).toEqual({ 'X-Requested-With': 'XMLHttpRequest' })
  })

  it('同時に再発行してもAPIは1回だけ呼び出す。', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, { accessToken: 'token', tokenType: 'Bearer', expiresIn: 300 }))

    const results = await Promise.all([refreshAccessToken(), refreshAccessToken()])

    expect(results).toEqual([true, true])
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('再発行できない場合は未ログインになる。', async () => {
    fetchMock.mockResolvedValue(jsonResponse(401, { code: 'UNAUTHENTICATED' }))

    const result = await refreshAccessToken()

    expect(result).toBe(false)
    expect(hasAccessToken()).toBe(false)
  })
})
