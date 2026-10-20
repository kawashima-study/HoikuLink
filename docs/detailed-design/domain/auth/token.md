# ドメイン詳細設計書：トークン（認証）

| 項目 | 内容 |
|---|---|
| 状態 | 作成中 |
| モジュール | 認証 |
| パッケージ | `io.github.kawashima_study.hoikulink.auth.domain` |
| 対象範囲 | Access Tokenの発行、Refresh Tokenの発行と保存 |

## 1. 概要

### 1-1. 集約名

RefreshToken（リフレッシュトークン）

### 1-2. 目的

ログインに成功した保育士に、Access Token（JWT）とRefresh Tokenを発行する。
Refresh TokenはOpaque Token（乱数の文字列）とし、DBにはハッシュ値だけを保存する。有効期限は最初のログインから12時間とする。

## 2. クラス一覧

| No. | クラス名 | 分類 | 概要 |
|---:|---|---|---|
| 1 | `RefreshToken` | 集約ルート（Entity） | Refresh Token。DBに保存する内容（ハッシュ値・トークンファミリー・有効期限）を持つ。 |
| 2 | `RefreshTokenId` | Value Object | リフレッシュトークンID（`RTK`＋ULID）。 |
| 3 | `RawRefreshToken` | Value Object | 生のRefresh Token（Cookieに入れる文字列）。 |
| 4 | `RefreshTokenHash` | Value Object | Refresh Tokenのハッシュ値（SHA-256）。 |
| 5 | `AccessToken` | Value Object | Access Token（JWT）と有効期限までの時間。 |
| 6 | `RefreshTokenGenerator` | インターフェース | 生のRefresh Tokenの作成。実装は `auth.adapter.out.crypto` に置き、`SecureRandom` で作る32バイトの乱数をBase64URLにした文字列とする。 |
| 7 | `AccessTokenIssuer` | インターフェース | Access Tokenの発行。実装は `auth.adapter.out.crypto` に置き、Spring Securityの `NimbusJwtEncoder` でHS256の署名をする。見出しに `kid`、中身に `iss`・`sub`（ユーザーID）・`iat`・`exp`（5分）・`jti`（乱数のUUID）を入れる。署名の鍵（Base64、32バイト以上）は環境変数 `HOIKULINK_JWT_SECRET` から読む。 |

## 3. 属性定義

### 3-1. RefreshToken

| No. | 項目名 | 型 | 必須 | 制約 | 備考 |
|---:|---|---|:---:|---|---|
| 1 | `refreshTokenId` | `RefreshTokenId` | ○ | | リフレッシュトークンを識別する。 |
| 2 | `userId` | `UserId` | ○ | | トークンを発行したアカウントのユーザーID。 |
| 3 | `tokenHash` | `RefreshTokenHash` | ○ | | 生のRefresh Tokenのハッシュ値。 |
| 4 | `familyId` | `RefreshTokenId` | ○ | | 1回のログインから交換で続いていくトークンのグループ。最初のトークンのIDとする。 |
| 5 | `expiresAt` | `Instant` | ○ | | 有効期限。 |
| 6 | `revokedAt` | `Instant` | | | 無効化日時。 |

### 3-2. RefreshTokenId

| No. | 項目名 | 型 | 必須 | 制約 | 備考 |
|---:|---|---|:---:|---|---|
| 1 | `value` | `String` | ○ | `RTK`＋ULIDの形式であること（`PrefixedUlid` のルール）。 | |

### 3-3. RawRefreshToken

| No. | 項目名 | 型 | 必須 | 制約 | 備考 |
|---:|---|---|:---:|---|---|
| 1 | `value` | `String` | ○ | | 生のRefresh Token。 |

### 3-4. RefreshTokenHash

| No. | 項目名 | 型 | 必須 | 制約 | 備考 |
|---:|---|---|:---:|---|---|
| 1 | `value` | `String` | ○ | 64文字の16進数（`0-9`・`a-f`）であること。 | SHA-256のハッシュ値を16進数の小文字にした文字列。 |

### 3-5. AccessToken

| No. | 項目名 | 型 | 必須 | 制約 | 備考 |
|---:|---|---|:---:|---|---|
| 1 | `value` | `String` | ○ | | Access Token（JWT）。 |
| 2 | `expiresIn` | `Duration` | ○ | | 有効期限までの時間（5分）。 |

## 4. メソッド定義

### 4-1. RefreshToken

| No. | メソッド名 | 引数 | 戻り値 | 例外 | 処理概要 |
|---:|---|---|---|---|---|
| 1 | コンストラクタ | `RefreshTokenId`、`UserId`、`RefreshTokenHash`、`RefreshTokenId`（トークンファミリーID）、`Instant`（有効期限）、`Instant`（無効化日時） | | `NullPointerException`（無効化日時以外が `null`） | 保存されている値から作成する。新しく発行する場合は `issue` を使う。 |
| 2 | `issue` | `UserId`、`RefreshTokenHash`、`Instant`（発行日時） | `RefreshToken` | `NullPointerException`（いずれかの引数が `null`） | 新しいリフレッシュトークンIDを作り、トークンファミリーIDを同じ値にする。有効期限は発行日時の12時間後とし、無効化日時は値なしとする。 |
| 3 | `rotate` | `RefreshTokenHash`（新しいトークンのハッシュ値）、`Instant`（交換日時） | `RefreshToken` | `IllegalStateException`（無効化済み、または有効期限切れ）、`NullPointerException`（交換日時が `null`） | このトークンの無効化日時を交換日時にし、同じトークンファミリーIDと同じ有効期限の新しいトークンを返す。 |
| 4 | `isUsable` | `Instant`（現在の時刻） | `boolean` | | 無効化されておらず、現在の時刻が有効期限より前の場合 `true` を返す。 |
| 5 | `isRevoked` | なし | `boolean` | | 無効化日時がある場合 `true` を返す。 |
| 6 | `refreshTokenId`、`userId`、`tokenHash`、`familyId`、`expiresAt` | なし | 各項目の型 | | 各項目の値を返す。 |
| 7 | `revokedAt` | なし | `Optional<Instant>` | | 無効化日時を返す。値がない場合は空を返す。 |

### 4-2. RefreshTokenId

| No. | メソッド名 | 引数 | 戻り値 | 例外 | 処理概要 |
|---:|---|---|---|---|---|
| 1 | コンストラクタ | `String` | | `NullPointerException`（`null`）、`InvalidIdException`（形式の誤り、またはプレフィックスが `RTK` でない） | 値を確認する。 |
| 2 | `newId` | なし | `RefreshTokenId` | | 新しいIDを作成する。 |
| 3 | `value` | なし | `String` | | 値を返す。 |

### 4-3. RawRefreshToken

| No. | メソッド名 | 引数 | 戻り値 | 例外 | 処理概要 |
|---:|---|---|---|---|---|
| 1 | コンストラクタ | `String` | | `NullPointerException`（`null`） | 値を確認する。 |
| 2 | `value` | なし | `String` | | 値を返す。 |
| 3 | `toString` | なし | `String` | | 中身を隠した文字列を返す（例：`RawRefreshToken[***]`）。 |

### 4-4. RefreshTokenHash

| No. | メソッド名 | 引数 | 戻り値 | 例外 | 処理概要 |
|---:|---|---|---|---|---|
| 1 | コンストラクタ | `String` | | `NullPointerException`（`null`）、`IllegalArgumentException`（形式の誤り） | 値を確認する。 |
| 2 | `of` | `RawRefreshToken` | `RefreshTokenHash` | `NullPointerException`（`null`） | 生のRefresh TokenをUTF-8のバイト列にし、Java標準の `MessageDigest` でSHA-256のハッシュ値を計算して、16進数の小文字の文字列にする。 |
| 3 | `value` | なし | `String` | | 値を返す。 |

### 4-5. AccessToken

| No. | メソッド名 | 引数 | 戻り値 | 例外 | 処理概要 |
|---:|---|---|---|---|---|
| 1 | コンストラクタ | `String`、`Duration` | | `NullPointerException`（`null`） | 値を確認する。 |
| 2 | `value`、`expiresIn` | なし | 各項目の型 | | 各項目の値を返す。 |
| 3 | `toString` | なし | `String` | | 中身を隠した文字列を返す（例：`AccessToken[***]`）。 |

### 4-6. RefreshTokenGenerator

| No. | メソッド名 | 引数 | 戻り値 | 例外 | 処理概要 |
|---:|---|---|---|---|---|
| 1 | `generate` | なし | `RawRefreshToken` | | 推測できない乱数の文字列を作成する。 |

### 4-7. AccessTokenIssuer

| No. | メソッド名 | 引数 | 戻り値 | 例外 | 処理概要 |
|---:|---|---|---|---|---|
| 1 | `issue` | `UserId`、`Instant`（発行日時） | `AccessToken` | | ユーザーIDのAccess Tokenを発行する。発行日時は呼び出す側が渡す。 |

## 5. 業務ルール（不変条件）

| No. | 業務ルール | 実装箇所 | 違反時の動作 |
|---:|---|---|---|
| 1 | Refresh Tokenの有効期限は、発行日時の12時間後とする。 | `RefreshToken.issue` | ― |
| 2 | ログインで発行したRefresh TokenのトークンファミリーIDは、そのトークン自身のIDとする。 | `RefreshToken.issue` | ― |
| 3 | DBには生のRefresh Tokenを保存せず、ハッシュ値だけを保存する。 | `RefreshToken`（生のトークンを持たない） | ― |
| 4 | 交換すると元のトークンを無効化し、新しいトークンは同じトークンファミリーIDと同じ有効期限を引き継ぐ（延長しない）。 | `RefreshToken.rotate` | ― |
| 5 | 無効化済みまたは有効期限切れのトークンは交換できない。 | `RefreshToken.rotate` | `IllegalStateException` |
| 6 | 無効化済みのトークンが使われた場合は、同じトークンファミリーのトークンをすべて無効化する。 | 再発行のユースケース（`RefreshService`）、`RefreshTokenRepository.revokeFamily` | ― |

## 6. 例外定義

| No. | 例外クラス | エラーコード | 発生条件 |
|---:|---|---|---|
| 1 | `NullPointerException` | ― | いずれかの値に `null` が渡された（プログラムの誤り）。 |
| 2 | `InvalidIdException`（`IllegalArgumentException` を継承） | ― | リフレッシュトークンIDの形式が誤っている、またはプレフィックスが `RTK` でない（`PrefixedUlid` のルール）。 |
| 3 | `IllegalArgumentException` | ― | ハッシュ値の形式が誤っている（プログラム、または保存データの誤り）。 |

## 7. テーブル対応

テーブル：`auth.refresh_tokens`

| No. | クラス・項目 | カラム |
|---:|---|---|
| 1 | `RefreshToken.refreshTokenId` | `refresh_token_id` |
| 2 | `RefreshToken.userId` | `user_id` |
| 3 | `RefreshTokenHash.value` | `token_hash` |
| 4 | `RefreshToken.familyId` | `family_id` |
| 5 | `RefreshToken.expiresAt` | `expires_at` |
| 6 | `RefreshToken.revokedAt` | `revoked_at` |

## 8. Repository

### 8-1. 配置

| No. | 種類 | 名前・置き場所 | 備考 |
|---:|---|---|---|
| 1 | インターフェース | `RefreshTokenRepository`（`auth.domain`） | 保存と読み出しのメソッドの定義のみを持つ。 |
| 2 | 実装 | `auth.adapter.out.persistence` | jOOQでDBを読み書きする。 |

### 8-2. メソッド定義

| No. | メソッド名 | 引数 | 戻り値 | 処理概要 |
|---:|---|---|---|---|
| 1 | `save` | `RefreshToken`、`UserId`（作成者）、`Instant`（作成日時） | なし | リフレッシュトークンを新しく登録する。作成者・更新者に作成者、作成日時・更新日時に作成日時を入れる。 |
| 2 | `findByHashForUpdate` | `RefreshTokenHash` | `Optional<RefreshToken>` | ハッシュ値でトークンを探し、その行をロックする。見つからない場合は空を返す。 |
| 3 | `update` | `RefreshToken`、`UserId`（更新者）、`Instant`（更新日時） | なし | 無効化日時・更新日時・更新者を書き込む。書き換える行がない場合は `IllegalStateException`。 |
| 4 | `revokeFamily` | `RefreshTokenId`（トークンファミリーID）、`UserId`（更新者）、`Instant`（無効化日時） | なし | 同じトークンファミリーのまだ無効化されていない行をすべて無効化する。無効化済みの行の無効化日時は変えない。 |

### 8-3. 処理の決まり

| No. | 項目 | 内容 |
|---:|---|---|
| 1 | 日時の変換 | `Instant` とDBの `DATETIME`（日本時間）の変換は、Repositoryの中で `BusinessTimeZone.JAPAN` を使って行う。 |
| 2 | 作成者 | ログインで発行する場合は、アカウント自身のユーザーIDとする。作成者と作成日時は、呼び出す側が渡す。 |

## 9. ログ出力

| No. | 対象 | 内容 |
|---:|---|---|
| 1 | `RawRefreshToken`・`AccessToken` の `toString` | 中身を出力しない。 |

## 10. 改訂履歴

| 版 | 日付 | 内容 |
|---|---|---|
| 1.0 | 2026-10-18 | 初版。 |
