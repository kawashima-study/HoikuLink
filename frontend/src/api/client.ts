// APIの呼び出し
// アクセストークンはブラウザのストレージに保存せずこのファイルの変数（メモリ）にだけ持つ

/** 画面から呼び出したことを示す独自の見出し（APIのCSRF対策で確認される） */
const CUSTOM_HEADER = { 'X-Requested-With': 'XMLHttpRequest' }

/** エラー応答（RFC 9457の形式）の本文 */
type ProblemBody = {
  title?: string
  detail?: string
  code?: string
}

/** ログインと再発行の成功の応答の本文 */
type AccessTokenBody = {
  accessToken: string
  tokenType: string
  expiresIn: number
}

/** APIがエラーを返したことを表す例外 */
export class ApiError extends Error {
  /** HTTPのステータス */
  readonly status: number

  /** エラーコード（例 INVALID_CREDENTIALS） */
  readonly code: string | undefined

  /** 画面に表示する詳細 */
  readonly detail: string | undefined

  constructor(status: number, body: ProblemBody) {
    super(body.title ?? 'エラーが発生しました。')
    this.status = status
    this.code = body.code
    this.detail = body.detail
  }
}

/** メモリに持つアクセストークン（ページを再読み込みすると消える） */
let accessToken: string | null = null

/** 実行中の再発行（同時に呼ばれても再発行は1回だけにする） */
let refreshing: Promise<boolean> | null = null

/** アクセストークンを持っているか */
export function hasAccessToken(): boolean {
  return accessToken !== null
}

/** メールアドレスとパスワードでログインする */
export async function login(email: string, password: string): Promise<void> {
  const response = await fetch('/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...CUSTOM_HEADER },
    body: JSON.stringify({ email, password }),
  })
  if (!response.ok) {
    throw await toApiError(response)
  }
  accessToken = ((await response.json()) as AccessTokenBody).accessToken
}

/**
 * リフレッシュトークン（Cookie）でアクセストークンを再発行する
 * 同時に呼ばれた場合は実行中の再発行の結果を共有する
 * 同じリフレッシュトークンで2回再発行すると使い回しと判定されログインが無効になるため
 */
export function refreshAccessToken(): Promise<boolean> {
  refreshing ??= doRefresh().finally(() => {
    refreshing = null
  })
  return refreshing
}

/** ログアウトする（失敗してもメモリのアクセストークンは消す） */
export async function logout(): Promise<void> {
  try {
    await fetch('/api/v1/auth/logout', { method: 'POST', headers: CUSTOM_HEADER })
  } finally {
    accessToken = null
  }
}

/**
 * アクセストークンを付けてAPIを呼び出す
 * 401の場合は1回だけ再発行してから呼び直す
 */
export async function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const response = await fetch(path, withAuthorization(init))
  if (response.status !== 401 || !(await refreshAccessToken())) {
    return response
  }
  return fetch(path, withAuthorization(init))
}

/** 再発行のAPIを呼び出しアクセストークンを入れ替える */
async function doRefresh(): Promise<boolean> {
  try {
    const response = await fetch('/api/v1/auth/refresh', { method: 'POST', headers: CUSTOM_HEADER })
    if (!response.ok) {
      accessToken = null
      return false
    }
    accessToken = ((await response.json()) as AccessTokenBody).accessToken
    return true
  } catch {
    // APIにつながらない場合も未ログインとして扱う
    accessToken = null
    return false
  }
}

/** リクエストにアクセストークンと独自の見出しを付ける */
function withAuthorization(init: RequestInit): RequestInit {
  const headers = new Headers(init.headers)
  headers.set('X-Requested-With', CUSTOM_HEADER['X-Requested-With'])
  if (accessToken !== null) {
    headers.set('Authorization', `Bearer ${accessToken}`)
  }
  return { ...init, headers }
}

/** エラー応答をApiErrorに変換する（本文がJSONでない場合も扱う） */
async function toApiError(response: Response): Promise<ApiError> {
  try {
    return new ApiError(response.status, (await response.json()) as ProblemBody)
  } catch {
    return new ApiError(response.status, {})
  }
}

/** テストのためにメモリの状態を初期化する（画面からは使わない） */
export function resetForTest(): void {
  accessToken = null
  refreshing = null
}
