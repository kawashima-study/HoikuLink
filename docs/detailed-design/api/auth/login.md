# API詳細設計書：ログイン（認証）

| 項目 | 内容 |
|---|---|
| 状態 | 作成中 |
| 機能ID | API-A-001 |
| モジュール | 認証 |
| 関連する設計書 | ドメイン詳細設計書（`domain/auth/user-account.md`・`domain/auth/token.md`）、テーブル定義（`auth.credentials`、`auth.refresh_tokens`） |

## 1. 機能概要

保育士がメールアドレスとパスワードでログインし、成功した場合はAccess Token（JWT）を応答の本文で返し、Refresh TokenをCookieに設定する。

<p>5回連続でログインに失敗したアカウントはロックし、失敗した場合の応答ではアカウントの有無を区別しない。

## 2. 権限制御

| No. | 項目 | 内容 |
|---|---|---|
| 1 | ログインの要否 | 不要（未ログインで呼び出す） |
| 2 | 呼び出しの回数の制限 | WAFで同じIPアドレスから5分間に100回までとする。 |

## 3. HTTPメソッド・エンドポイント

| 項目 | 内容 |
|---|---|
| HTTPメソッド | `POST` |
| エンドポイント | `/api/v1/auth/login` |

## 4. リクエスト

### 4-1. ヘッダー

| No. | 項目名 | 必須 | 内容 |
|---:|---|:---:|---|
| 1 | `Content-Type` | ○ | `application/json` |

### 4-2. ボディ

| No. | 項目名 | 型 | 必須 | 内容 |
|---:|---|---|:---:|---|
| 1 | `email` | 文字列 | ○ | ログインID（メールアドレス） |
| 2 | `password` | 文字列 | ○ | パスワード |

## 5. レスポンス

### 5-1. 成功（200）

**ヘッダー**

| No. | 項目名 | 内容 |
|---:|---|---|
| 1 | `Set-Cookie` | Refresh Token<br>名前：`refresh_token`<br>属性： `HttpOnly`・`Secure`・`SameSite=Strict`・`Path=/api/v1/auth`<br>有効期限はログインから12時間 |

**ボディ**

| No. | 項目名 | 型 | 内容 |
|---:|---|---|---|
| 1 | `accessToken` | 文字列 | Access Token（JWT） |
| 2 | `tokenType` | 文字列 | `Bearer` |
| 3 | `expiresIn` | 数値 | Access Tokenの有効期限までの秒数（300） |

### 5-2. 失敗

RFC 9457の形式（`type`・`title`・`status`・`detail`・`code`・`requestId`）で返す。HTTPのステータスとエラーコードは10章のとおり。

## 6. 処理フロー

| No. | 処理 | 詳細 |
|---:|---|---|
| 1 | 入力チェック | 7章のとおり確認する。 |
| 2 | アカウントの読み込み | `UserAccountRepository.findByEmailForUpdate` で読み込み、行をロックする。 |
| 3 | アカウントがない場合 | ダミーのハッシュ値でパスワードを照合して処理時間をそろえ、ログインの失敗として応答する。 |
| 4 | パスワードの照合 | パスワードの照合のインターフェース（Argon2id）で、入力されたパスワードと保存されているハッシュ値を照合する。 |
| 5 | ロック中の場合 | 照合の結果にかかわらず失敗回数とロックの状態は変えない。パスワードが正しい場合はロック中であることを、正しくない場合はログインの失敗を応答する。 |
| 6 | パスワードが正しくない場合 | `UserAccount.recordLoginFailure` で失敗を記録し、`UserAccountRepository.update` で書き込む。ログインの失敗として応答する。 |
| 7 | パスワードが正しい場合 | `UserAccount.recordLoginSuccess` で失敗回数を0に戻し、`UserAccountRepository.update` で書き込む。 |
| 8 | トークンの発行 | `AccessTokenIssuer.issue` でAccess Token（JWT）を発行する。`RefreshTokenGenerator.generate` で生のRefresh Tokenを作り、`RefreshTokenHash.of` でハッシュ値（SHA-256）にして、`RefreshToken.issue` で作成する。`RefreshTokenRepository.save` で `auth.refresh_tokens` に保存する。 |
| 9 | 応答 | 5-1のとおり返す。 |

- ログインのユースケースは例外を投げず、成功・失敗・ロック中を表す結果を返す。APIの側で結果を応答に変換する。
- 書き込みの更新者・作成者はアカウント自身のユーザーIDとする。日時は `Clock` から取得。
- No.1のうち、必須の確認はAPIにて行う。メールアドレスの長さ・形式の確認はユースケースの中で `EmailAddress` を作るときに行う（書き込みの前のため、例外で取り消される書き込みはない）。

### 6-1. ユースケースの構成

| No. | 名前 | 種類 | 置き場所 | 内容 |
|---:|---|---|---|---|
| 1 | `LoginUseCase` | インターフェース | `auth.application` | ログインのユースケースの入口。APIの側はこのインターフェースだけを使う。メソッドは `login(LoginCommand)` で、`LoginResult` を返す。 |
| 2 | `LoginService` | クラス（`LoginUseCase` の実装） | `auth.application` | 処理フローNo.2〜8を行う。メソッドに `@Transactional` を付ける。 |
| 3 | `LoginCommand` | record | `auth.application` | 入力。`email`（文字列）・`password`（文字列）。ユースケースの中で `EmailAddress`・`RawPassword` に変える。 |
| 4 | `LoginResult` | sealed interface | `auth.application` | 結果。次の3種類のrecordのいずれか。 |

| No. | 結果の種類 | 持つ値 | 条件 | APIの応答 |
|---:|---|---|---|---|
| 1 | `LoginResult.Success` | `AccessToken`、`RawRefreshToken`、`Instant`（Refresh Tokenの有効期限） | パスワードが正しく、ロック中でない | 200（5-1） |
| 2 | `LoginResult.InvalidCredentials` | なし | アカウントがない、またはパスワードが正しくない | 401 `INVALID_CREDENTIALS` |
| 3 | `LoginResult.AccountLocked` | なし | ロック中でパスワードが正しい | 401 `ACCOUNT_LOCKED` |

## 7. バリデーションチェック

| No. | 項目 | チェック | エラー時の応答 |
|---:|---|---|---|
| 1 | `email` | 必須（空文字列・空白のみを含む） | 422 `VALIDATION_ERROR` |
| 2 | `email` | メールアドレスの長さ・形式（`EmailAddress` のルール） | 422 `INVALID_EMAIL_ADDRESS` |
| 3 | `password` | 必須（空文字列を含む） | 422 `VALIDATION_ERROR` |

- パスワードの長さ（15〜64文字）は、ログインでは確認しない。ルールを変えた場合に、変更前のパスワードでログインできなくなるため。

## 8. データ更新

| No. | テーブル | 操作 | 条件 | 内容 |
|---:|---|---|---|---|
| 1 | `auth.credentials` | 参照（行のロック） | 処理フローNo.2 | メールアドレスが一致し、削除されていない行 |
| 2 | `auth.credentials` | 更新 | 処理フローNo.6・7 | 失敗回数・ロック日時・解除日時・解除者・更新日時・更新者 |
| 3 | `auth.refresh_tokens` | 登録 | 処理フローNo.8 | Refresh Tokenのハッシュ値・有効期限など |

## 9. トランザクション

処理フローNo.2〜8を1つのトランザクションで行う。ロックはトランザクションの終わりまで続く。ログインの失敗でも、失敗回数の書き込みを確定させる（例外でトランザクションを取り消さない）。

## 10. エラー処理

| No. | 条件 | HTTPのステータス | エラーコード | タイトル（`title`） | 詳細（`detail`） |
|---:|---|---:|---|---|---|
| 1 | アカウントがない、またはパスワードが正しくない（ロック中でパスワードが正しくない場合を含む） | 401 | `INVALID_CREDENTIALS` | メールアドレスまたはパスワードが正しくありません。 | 入力内容を確認してもう一度お試しください。 |
| 2 | ロック中でパスワードが正しい | 401 | `ACCOUNT_LOCKED` | アカウントがロックされています。 | 園長または主任にロックの解除を依頼してください。 |
| 3 | 入力チェックの誤り | 422 | `VALIDATION_ERROR`・`INVALID_EMAIL_ADDRESS` | 7章のとおり | 7章のとおり |
| 4 | 想定外のエラー | 500 | `INTERNAL_ERROR` | 共通の定義のとおり | 返さない |

- `INVALID_CREDENTIALS`・`ACCOUNT_LOCKED` は `AuthErrorCode` に追加する。

## 11. ログ出力

| No. | タイミング | レベル | 内容 |
|---:|---|---|---|
| 1 | ログインの成功 | INFO | ユーザーID |
| 2 | ログインの失敗（アカウントあり） | INFO | ユーザーID・失敗回数 |
| 3 | ログインの失敗（アカウントなし） | INFO | アカウントがないこと（メールアドレスは出力しない） |
| 4 | ロック | WARN | ユーザーID |

- メールアドレス・パスワード・トークンは、どの環境でもログに出力しない。

## 12. 改訂履歴

| 版 | 日付 | 内容 |
|---|---|---|
| 1.0 | 2026-10-17 | 初版。 |
