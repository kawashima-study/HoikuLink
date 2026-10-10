# API一覧（公開版・修正版 第3版）

- 作成日：2026-10-07
- 第3版の変更：URLにバージョン（`/api/v1`）を付けた（論点C3）。HTTPステータスの使い分けと、エラーの形式の詳細を追加（論点C1）。トップに連携事項を表示（ADR-93）
- 第2版の変更：連絡帳の検索を、保育士連絡帳・保護者連絡帳の両方を1つのAPIで検索する形に変更。エラーの形式（論点T：T1）とページング（論点U：U1）を確定

## 共通ルール

| 項目 | ルール |
|---|---|
| URLの入口 | 認証：`/api/v1/auth/...`、保育士Web：`/api/v1/teacher/...`、保護者アプリ（将来）：`/api/v1/parent/...`（ADR-83） |
| バージョン | URLに `v1` を含める。互換性のない変更をするときは `v2` を並べて出す（ADR-83） |
| URLの書き方 | リソースを名詞で表し、動詞を入れない（例：`/api/v1/teacher/teacher-diaries`）。操作はHTTPメソッドで表す（取得：GET、作成：POST、更新：PUT、部分更新：PATCH、削除：DELETE） |
| 個人情報 | 氏名などの個人情報をURLやクエリパラメータに含めない。検索の条件は `POST .../search` の本文で送る（URLはログやブラウザの履歴に残るため） |
| 確認の順番 | HTTP・JSONの形式 → 認証（JWT） → ユーザーの特定 → `X-Nursery-Id` → 園の状態（閉園なら拒否） → 所属・権限・閲覧範囲 → 業務処理。JWTが有効でも、選択中の園が使えるとは限らない（ADR-69、ADR-78） |
| 認証 | ログイン・Access Tokenの再発行以外は `Authorization: Bearer <Access Token>` が必須 |
| 選択中の園 | `/api/v1/teacher/...` は `X-Nursery-Id` ヘッダーが必須（選択できる園の一覧の取得を除く）。サーバーが所属を確認する（ADR-41） |
| 全APIの共通の確認 | 園運営のSpring Securityのフィルターで、所属・閉園・退職・無操作15分・初期パスワードを確認する（ADR-78） |
| CSRF | Access Tokenの再発行・ログアウトは、独自ヘッダーとOriginヘッダーも確認する（ADR-36） |
| 楽観的ロック | 更新（`PUT`）と画像の追加・削除は、読み込んだときの `version` を送る。食い違ったら409 |
| 名簿の外のID | 404（存在しないものとして扱う。3-6）。URLの中のIDの形式が違う場合も404 |
| エラーの形式 | RFC 9457（Problem Details）＋独自のエラーコード（下記。ADR-90）。HTTPメソッドやステータスによって形式を変えない |
| ページング | 下記（ADR-91） |

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
| `type` | エラーの種類を表すURI。`urn:hoikulink:error:` ＋エラーコードを小文字とハイフンにしたもの（公開後に、説明のページのURLへ変更予定。画面は `code` で処理を分ける） |
| `title` | エラーの種類の短い説明（画面に表示できる文） |
| `status` | HTTPのステータスコード |
| `detail` | 今回のエラーの具体的な説明 |
| `code` | このシステム独自のエラーコード（画面が処理を分けるのに使う） |
| `requestId` | リクエストごとのID。ログにも同じIDを出す（問い合わせのときに、ログの該当の行を探すため） |
| `errors` | 入力チェックのエラーのときだけ。項目ごとのエラーの一覧（`[{ "field": "temperature", "code": "OUT_OF_RANGE", "message": "34.0〜43.0で入力してください" }]`） |

### HTTPステータスの使い分け

| ステータス | 意味 | `code` の例 |
|---|---|---|
| 200 OK | 正常（取得・更新） | ― |
| 201 Created | 作成した | ― |
| 204 No Content | 正常（返す本文なし。削除など） | ― |
| 400 Bad Request | リクエストの形式そのものが不正（JSONが壊れている、必須のヘッダーがない、Content-Typeが違う） | `INVALID_REQUEST` |
| 401 Unauthorized | 認証情報がない・無効（ログイン画面へ） | `UNAUTHENTICATED` |
| 403 Forbidden | 認証済みだが権限がない、選択中の園を使えない（閉園・退職。園の選択画面へ） | `FORBIDDEN`、`NURSERY_CLOSED` |
| 404 Not Found | 対象がない、または閲覧範囲の外 | `RESOURCE_NOT_FOUND` |
| 409 Conflict | 状態の競合（楽観的ロック、1園児1日1件の重複） | `DIARY_VERSION_CONFLICT` |
| 422 Unprocessable Content | 形式は正しいが、入力値・業務ルールで処理できない（体温の範囲外、文字数の超過、未来の日付） | `VALIDATION_ERROR` |
| 429 Too Many Requests | 回数の制限（WAF） | `TOO_MANY_REQUESTS` |
| 500 Internal Server Error | サーバー内部のエラー（詳細は返さない） | `INTERNAL_ERROR` |
| 503 Service Unavailable | 一時的に使えない（Redisの障害時など） | `SERVICE_UNAVAILABLE` |

ログイン失敗のメッセージは、アカウントの存在やロックの有無を区別しない（ADR-39）。

### ページング（検索のAPI）

| 項目 | 内容 |
|---|---|
| 引数 | `page`（1から）。1ページの件数は50件で固定（画面からは変えられない） |
| 上限 | 最大2,000件、最大40ページ。2,000件を超える場合は「条件を絞ってください」と表示する |
| 件数の数え方 | 2,001件目まで数えて打ち切る（全件を数えない。13-5） |
| 返す値 | `items`（一覧）、`page`（現在のページ）、`totalCount`（件数。2,000件まで）、`totalPages`（総ページ数。最大40）、`overLimit`（2,000件を超えたか） |
| 並び順 | 対象日の新しい順、同じ日はIDの順（必ず一意になる並び順。13-5） |

## 認証（共通）

| 機能ID | 機能名 | 概要 | メソッド | エンドポイント | 備考 |
|---|---|---|---|---|---|
| API-A-001 | ログイン | メールアドレスとパスワードで認証し、Access Tokenを返す。Refresh TokenはHttpOnly Cookieで返す | POST | `/api/v1/auth/login` | 5回失敗でロック |
| API-A-002 | Access Tokenの再発行 | Refresh Tokenを交換し、新しいAccess Tokenを返す | POST | `/api/v1/auth/refresh` | 使い回しを検知したらトークンファミリーごと無効化 |
| API-A-003 | ログアウト | Refresh Tokenを無効化し、使用中のAccess Tokenを即時無効化する | POST | `/api/v1/auth/logout` | ADR-40 |
| API-A-004 | パスワード変更 | 現在のパスワードを確認して変更する。初期パスワードの強制変更にも使う | PUT | `/api/v1/auth/password` | 過去5世代・漏洩照合・禁止パターン。変更後は全端末のRefresh Tokenを無効化 |

## 保育士Web

| 機能ID | 機能名 | 概要 | メソッド | エンドポイント | 備考 |
|---|---|---|---|---|---|
| API-T-001 | 選択できる園の一覧 | ログインした人が所属する園（閉園を除く）を返す | GET | `/api/v1/teacher/nurseries` | `X-Nursery-Id` 不要。1園だけなら画面で自動選択 |
| API-T-002 | 状態の確認 | 25秒ごとのPolling。在籍状態・権限のバージョンを返す | GET | `/api/v1/teacher/session-status` | 401：ログイン画面へ、403：園の選択画面へ。無操作の時刻を更新しない |
| API-T-003 | トップ情報取得 | 閲覧範囲の園児と最新の連絡帳を返す | GET | `/api/v1/teacher/top` | ViewableScope。見てよい園児の直近7日間の連携事項（新しい順）も返す（ADR-93） |
| API-T-004 | 園児一覧取得 | 条件を指定して園児の一覧を返す | POST | `/api/v1/teacher/children/search` | ViewableScope |
| API-T-005 | 連絡帳の検索 | 条件を指定して、保育士連絡帳・保護者連絡帳の両方を1つの一覧で返す。各行に種類（保育士／保護者）を含める | POST | `/api/v1/teacher/diaries/search` | 保育士連絡帳は `TeacherDiaryScope`、保護者連絡帳は `ParentDiaryScope` で絞る。ページングは共通ルール（ADR-89） |
| API-T-006 | 保育士連絡帳の詳細 | 指定した保育士連絡帳を返す。画像は30秒の署名付きURLを含める | GET | `/api/v1/teacher/teacher-diaries/{teacherDiaryId}` | 名簿の外は404 |
| API-T-007 | 保育士連絡帳の作成 | 保育士連絡帳を作成する | POST | `/api/v1/teacher/teacher-diaries` | 1園児1日1件。スナップショットを保存 |
| API-T-008 | 保育士連絡帳の編集 | 指定した保育士連絡帳を更新する | PUT | `/api/v1/teacher/teacher-diaries/{teacherDiaryId}` | `version` 必須。409で競合 |
| API-T-009 | 保育士連絡帳の削除 | 指定した保育士連絡帳を削除する | DELETE | `/api/v1/teacher/teacher-diaries/{teacherDiaryId}` | `OWN`／`ALL` の権限 |
| API-T-010 | 保育士連絡帳の画像追加 | 画像を受け取り、位置情報を除去してS3に保存する | POST | `/api/v1/teacher/teacher-diaries/{teacherDiaryId}/images` | multipart。サイズ・中身の検査（ADR-56）。1連絡帳5枚まで |
| API-T-011 | 保育士連絡帳の画像削除 | 指定した画像を削除する | DELETE | `/api/v1/teacher/teacher-diaries/{teacherDiaryId}/images/{imageId}` | 連絡帳の `version` が上がる |
| API-T-013 | 保護者連絡帳の詳細 | 指定した保護者連絡帳を返す。画像は30秒の署名付きURLを含める | GET | `/api/v1/teacher/parent-diaries/{parentDiaryId}` | 名簿の外は404 |
| API-T-014 | 保護者連絡帳の削除 | 指定した保護者連絡帳を削除する | DELETE | `/api/v1/teacher/parent-diaries/{parentDiaryId}` | `PARENT_DIARY_DELETE` |
| API-T-015 | 保護者連絡帳の画像削除 | 指定した画像を削除する | DELETE | `/api/v1/teacher/parent-diaries/{parentDiaryId}/images/{imageId}` | `PARENT_DIARY_IMAGE_DELETE`。文章の編集APIはない |
| API-T-016 | ロック中のアカウントの一覧 | 選択中の園に所属する保育士のうち、ロック中のものを返す | GET | `/api/v1/teacher/management/locked-accounts` | `ACCOUNT_UNLOCK`（園長・主任） |
| API-T-017 | ロックの解除 | 指定したアカウントのロックを解除する | POST | `/api/v1/teacher/management/locked-accounts/{userId}/unlock` | 監査ログに残す |

API-T-012（保護者連絡帳の検索）は、第2版でAPI-T-005に統合したため欠番。

## 修正点の一覧

| 版 | 元 | 修正 | 理由 |
|---|---|---|---|
| 第1版 | `/api/login`、`/api/logout` | `/api/v1/auth/...` | 認証の入口を分ける |
| 第1版 | `/api/diaries`（保育士連絡帳と保護者連絡帳の区別なし） | 詳細・作成・編集・削除は `/api/v1/teacher/teacher-diaries`、`/api/v1/teacher/parent-diaries` に分ける | 論点O、ADR-04 |
| 第1版 | トップ情報取得（POST） | GET | 条件がなく、取得だけのため |
| 第1版 | ― | Access Tokenの再発行、パスワード変更、園の一覧、状態の確認、画像、保護者連絡帳、ロック解除を追加 | 公開版の機能に必要 |
| 第2版 | 保育士連絡帳の検索・保護者連絡帳の検索（2つ） | 連絡帳の検索（1つ）に統合 | 連絡帳一覧の画面で両方をまとめて検索するため（ADR-89） |
| 第2版 | エラーの形式・ページングが未定 | RFC 9457、1ページ50件・最大40ページ | 論点T・U（ADR-90、ADR-91） |
| 第3版 | `/api/auth/...`、`/api/teacher/...` | `/api/v1/auth/...`、`/api/v1/teacher/...` | 論点C3（ADR-83） |
| 第3版 | ― | HTTPステータスの使い分け、URLの書き方、個人情報、確認の順番を追加 | 論点C1（ADR-90） |
| 第3版 | トップ情報取得 | 連携事項も返す | ADR-93 |
