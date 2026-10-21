# API一覧

- 作成日：2026-10-07

## 共通ルール

| 項目 | ルール |
|---|---|
| URLの入口 | 認証：`/api/v1/auth/...`、保育士Web：`/api/v1/teacher/...`、保護者アプリ（将来）：`/api/v1/parent/...`。 |
| バージョン | URLに`v1`を付与。互換性のない変更の場合は`v2`を並行して提供。 |
| URLの書き方 | リソースを名詞で表し、動詞を含めない（例：`/api/v1/teacher/teacher-diaries`）。操作はHTTPメソッドで表現（取得：GET、作成：POST、更新：PUT、部分更新：PATCH、削除：DELETE）。 |
| 個人情報 | 氏名などの個人情報は、URLやクエリパラメータに記載不可。検索の条件は`POST .../search`の本文で送信（URLはログやブラウザの履歴に残るため）。 |
| 確認の順番 | HTTP・JSONの形式 → 認証（JWT） → ユーザーの特定 →`X-Nursery-Id`→ 園の状態（閉園の場合は拒否） → 所属・権限・閲覧範囲 → 業務処理。JWTが有効でも、選択中の園が利用可能とは限らないため、園の状態も確認。 |
| 認証 | ログイン・Access Tokenの再発行以外は`Authorization: Bearer <Access Token>`が必須。 |
| 選択中の園 | `/api/v1/teacher/...`は`X-Nursery-Id`ヘッダーが必須（選択できる園の一覧の取得を除く）。サーバーで所属を確認。 |
| 全APIの共通の確認 | 園運営モジュールのSpring Securityのフィルターで、所属・閉園・退職・無操作15分・初期パスワードを確認。 |
| CSRF | Access Tokenの再発行・ログアウトは、カスタムヘッダーとOriginヘッダーも確認。 |
| 楽観的ロック | 更新（`PUT`）と画像の追加・削除は、読み込み時の`version`を送信。一致しない場合は409。 |
| 閲覧範囲の外のID | 404（存在しないものとして扱う）。URLの中のIDの形式が不正な場合も404。 |
| エラーの形式 | RFC 9457（Problem Details）＋独自のエラーコード（下記）。HTTPメソッドやステータスにかかわらず形式は共通。 |
| ページング | 下記。 |

### エラーの形式（RFC 9457 Problem Details）

```json
{
  "type": "urn:hoikulink:error:diary-version-conflict",
  "title": "他の人が先に更新しました",
  "status": 409,
  "detail": "最新の内容を読み込み直してから、もう一度編集してください",
  "code": "DIARY_VERSION_CONFLICT",
  "requestId": "（リクエストごとのID）"
}
```

| 項目 | 内容 |
|---|---|
| `type` | エラーの種類を表すURI。`urn:hoikulink:error:`＋エラーコードを小文字とハイフンにしたもの。画面は`code`で処理を分岐。 |
| `title` | エラーの種類の短い説明（画面に表示できる文）。 |
| `status` | HTTPのステータスコード。 |
| `detail` | 該当のエラーの具体的な説明。 |
| `code` | このシステム独自のエラーコード（画面での処理の分岐に使用）。 |
| `requestId` | リクエストごとのID。ログにも同じIDを出力（問い合わせの際に、ログの該当の行を特定するため）。 |
| `errors` | 入力チェックのエラーの場合のみ。項目ごとのエラーの一覧（`[{ "field": "temperature", "code": "OUT_OF_RANGE", "message": "34.0〜43.0で入力してください" }]`）。 |

### HTTPステータスの使い分け

| ステータス | 意味 | `code`の例 |
|---|---|---|
| 200 OK | 正常（取得・更新）。 | ― |
| 201 Created | 作成の完了。 | ― |
| 204 No Content | 正常（返す本文なし。削除など）。 | ― |
| 400 Bad Request | リクエストの形式そのものが不正（JSONの形式の不正、必須のヘッダーの欠落、Content-Typeの不一致）。 | `INVALID_REQUEST` |
| 401 Unauthorized | 認証情報の欠落・無効（ログイン画面へ遷移）。 | `UNAUTHENTICATED` |
| 403 Forbidden | 認証済みだが権限がない、または選択中の園が利用不可（閉園・退職。園の選択画面へ遷移）。 | `FORBIDDEN`、`NURSERY_CLOSED` |
| 404 Not Found | 対象が存在しない、または閲覧範囲の外。 | `RESOURCE_NOT_FOUND` |
| 409 Conflict | 状態の競合（楽観的ロック、1園児1日1件の重複）。 | `DIARY_VERSION_CONFLICT` |
| 422 Unprocessable Content | 形式は正しいが、入力値・業務ルールにより処理不可（体温の範囲外、文字数の超過、未来の日付）。 | `VALIDATION_ERROR` |
| 429 Too Many Requests | 回数の制限（WAF）。 | `TOO_MANY_REQUESTS` |
| 500 Internal Server Error | サーバー内部のエラー（詳細は返さない）。 | `INTERNAL_ERROR` |
| 503 Service Unavailable | 一時的な利用不可（Redisの障害時など）。 | `SERVICE_UNAVAILABLE` |

ログインの失敗のメッセージは、アカウントの有無によらず共通。ロック中は、パスワードが正しい場合のみロックされていることを通知する（パスワードを知らない人には、アカウントの有無が分からない）。

### ページング（検索のAPI）

| 項目 | 内容 |
|---|---|
| 引数 | `page`（1から）。1ページの件数は50件で固定（画面からの変更不可）。 |
| 上限 | 最大2,000件、最大40ページ。2,000件を超える場合は「条件を絞ってください」と表示。 |
| 件数の数え方 | 2,001件目まで数えて打ち切り（全件は数えない）。 |
| 返す値 | `items`（一覧）、`page`（現在のページ）、`totalCount`（件数。2,000件まで）、`totalPages`（総ページ数。最大40）、`overLimit`（2,000件を超えたか）。 |
| 並び順 | 対象日の新しい順、同じ日はIDの順（必ず一意になる並び順）。 |

## 認証（共通）

| 機能ID | 機能名 | 概要 | メソッド | エンドポイント | 備考 |
|---|---|---|---|---|---|
| API-A-001 | ログイン | メールアドレスとパスワードで認証し、Access Tokenを返却。Refresh TokenはHttpOnly Cookieで返却。 | POST | `/api/v1/auth/login` | 5回の失敗でロック。 |
| API-A-002 | Access Tokenの再発行 | Refresh Tokenを交換し、新しいAccess Tokenを返却。 | POST | `/api/v1/auth/refresh` | 使い回しを検知した場合はトークンファミリーごと無効化。 |
| API-A-003 | ログアウト | Refresh Tokenを無効化し、使用中のAccess Tokenを即時に無効化。 | POST | `/api/v1/auth/logout` | 使用中のAccess TokenのID（jti）を、残りの有効期限の間Redisに登録して無効化。 |
| API-A-004 | パスワード変更 | 現在のパスワードを確認して変更。初期パスワードの強制変更にも使用。 | PUT | `/api/v1/auth/password` | 過去5世代・漏洩照合・禁止パターン。変更後は全端末のRefresh Tokenを無効化。 |

## 保育士Web

| 機能ID | 機能名 | 概要 | メソッド | エンドポイント | 備考 |
|---|---|---|---|---|---|
| API-T-001 | 選択できる園の一覧 | ログインした利用者が所属する園（閉園を除く）を返却。 | GET | `/api/v1/teacher/nurseries` | `X-Nursery-Id`は不要。1園のみの場合は画面で自動選択。 |
| API-T-002 | 状態の確認 | 25秒ごとのPolling。在籍状態・権限のバージョンを返却。 | GET | `/api/v1/teacher/session-status` | 401：ログイン画面へ、403：園の選択画面へ遷移。無操作の時刻は更新対象外。 |
| API-T-003 | トップ情報取得 | 閲覧範囲の園児と最新の連絡帳を返却。 | GET | `/api/v1/teacher/top` | ViewableScope。閲覧範囲の園児の直近7日間の連携事項（新しい順）も返却。 |
| API-T-004 | 園児一覧取得 | 条件を指定して園児の一覧を返却。 | POST | `/api/v1/teacher/children/search` | ViewableScope。 |
| API-T-005 | 連絡帳の検索 | 条件を指定して、保育士連絡帳・保護者連絡帳の両方を1つの一覧で返却。各行に種類（保育士／保護者）を付与。 | POST | `/api/v1/teacher/diaries/search` | 保育士連絡帳は`TeacherDiaryScope`、保護者連絡帳は`ParentDiaryScope`で絞り込み。ページングは共通ルール。 |
| API-T-006 | 保育士連絡帳の詳細 | 指定した保育士連絡帳を返却。画像は30秒の署名付きURLを付与。 | GET | `/api/v1/teacher/teacher-diaries/{teacherDiaryId}` | 閲覧範囲の外は404。 |
| API-T-007 | 保育士連絡帳の作成 | 保育士連絡帳を作成。 | POST | `/api/v1/teacher/teacher-diaries` | 1園児1日1件。スナップショットを保存。 |
| API-T-008 | 保育士連絡帳の編集 | 指定した保育士連絡帳を更新。 | PUT | `/api/v1/teacher/teacher-diaries/{teacherDiaryId}` | `version`は必須。競合の場合は409。 |
| API-T-009 | 保育士連絡帳の削除 | 指定した保育士連絡帳を削除。 | DELETE | `/api/v1/teacher/teacher-diaries/{teacherDiaryId}` | `OWN`／`ALL`の権限。 |
| API-T-010 | 保育士連絡帳の画像追加 | 画像を受け取り、位置情報を除去してS3に保存。 | POST | `/api/v1/teacher/teacher-diaries/{teacherDiaryId}/images` | multipart。サイズ・中身の検査。1連絡帳5枚まで。 |
| API-T-011 | 保育士連絡帳の画像削除 | 指定した画像を削除。 | DELETE | `/api/v1/teacher/teacher-diaries/{teacherDiaryId}/images/{imageId}` | 連絡帳の`version`を更新。 |
| API-T-012 | 保護者連絡帳の詳細 | 指定した保護者連絡帳を返却。画像は30秒の署名付きURLを付与。 | GET | `/api/v1/teacher/parent-diaries/{parentDiaryId}` | 閲覧範囲の外は404。 |
| API-T-013 | 保護者連絡帳の削除 | 指定した保護者連絡帳を削除。 | DELETE | `/api/v1/teacher/parent-diaries/{parentDiaryId}` | `PARENT_DIARY_DELETE`。 |
| API-T-014 | 保護者連絡帳の画像削除 | 指定した画像を削除。 | DELETE | `/api/v1/teacher/parent-diaries/{parentDiaryId}/images/{imageId}` | `PARENT_DIARY_IMAGE_DELETE`。文章の編集のAPIはなし。 |
| API-T-015 | ロック中のアカウントの一覧 | 選択中の園に所属する保育士のうち、ロック中のアカウントを返却。 | GET | `/api/v1/teacher/management/locked-accounts` | `ACCOUNT_UNLOCK`（園長・主任）。 |
| API-T-016 | ロックの解除 | 指定したアカウントのロックを解除。 | POST | `/api/v1/teacher/management/locked-accounts/{userId}/unlock` | 監査ログに記録。 |
