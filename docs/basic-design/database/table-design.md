# テーブル定義（公開版・修正版 第3版）

- 作成日：2026-10-07
- 元にした設計書：NurseryDiary_設計整理_現時点_V10_9.md
- 第2版の変更：日時の型、Enum（MySQLの `ENUM` 型）、マスタの名前（`mst_`）、論理名、備考の書き方、制約・インデックスの書き方をそろえた。連絡帳の項目を画面の項目に合わせた（末尾の「第2版の変更点」）
- 第3版の変更：選択肢をJavaの `enum`＋DBは文字（`VARCHAR`）に変更。論点X（秒まで）・Y（園コードは追加要件）・Z（メイン保護者は必須）・AA（作成保育士IDと作成者を分ける）、選択肢の値・文字数・画像の上限を確定

## 共通ルール

| 項目 | ルール | 根拠 |
|---|---|---|
| スキーマ | モジュールごとに分ける：`auth`（認証）、`nursery`（園運営）、`diary`（連絡帳） | 2-4 |
| ID | 3文字のプレフィックス＋ULID（26文字）。型は `CHAR(29)` | ADR-47 |
| 日時 | `DATETIME`（秒まで）。UTCで保存し、アプリが値を設定する | ADR-81 |
| 日付 | `DATE`。日本の暦日（連絡帳の対象日、所属開始日・終了日、契約終了日など、日付だけで足りるもの） | ADR-81 |
| 時刻 | `TIME`。日本時間の時刻（検温時刻、入眠・起床の時刻など） | ― |
| 選択肢 | Javaの `enum` で管理し、DBには `enum` の名前を文字（`VARCHAR(20)`）で保存する。並び順の番号では保存しない。値の追加・並べ替えはJavaの `enum` を変えるだけで、DBの変更は不要 | ADR-84 |
| マスタ | 全園共通で、アプリ（Flyway）が投入する固定のデータは、論理名に「マスタ」、物理名に `mst_` を付ける | ― |
| 文字コード | `utf8mb4`（照合順序は環境構築時に決定） | ― |
| 論理削除 | `deleted_at` に削除日時を入れる | ADR-50 |
| 論理削除とユニーク制約 | 生成列 `active_flag`（削除されていなければ1、削除済みはNULL）をユニーク制約に含める | ADR-80 |
| 外部キー | 同じモジュールの中の、データが少ないテーブルだけに付ける | ADR-82 |

### 共通の項目（備考に「共通：…」と書いたもの）

| 項目 | 論理名 | 物理名 | 型 | NN | 内容 |
|---|---|---|---|:-:|---|
| 監査項目 | 作成日時 | created_at | DATETIME | ○ | 行を作成した日時（UTC） |
| 監査項目 | 作成者 | created_by | VARCHAR(29) | ○ | 操作したユーザーID。バッチ・初期データは `SYSTEM` |
| 監査項目 | 更新日時 | updated_at | DATETIME | ○ | 最後に更新した日時。作成時は作成日時と同じ値を入れる |
| 監査項目 | 更新者 | updated_by | VARCHAR(29) | ○ | 最後に更新したユーザーID。作成時は作成者と同じ値を入れる |
| 論理削除 | 削除日時 | deleted_at | DATETIME | | 削除した日時。削除されていなければNULL |
| ユニーク制約用 | 有効フラグ | active_flag | TINYINT（生成列） | | `deleted_at` がNULLなら1、それ以外はNULL。DBが自動で計算する（ADR-80） |

### 備考の書き方

| 種類 | 書き方の例 |
|---|---|
| 主キーのID | `USR`＋ULID |
| 同じモジュールの外部キー | 参照先：credentials.user_id |
| 外部キーを付けない参照 | 参照先：nursery.children.child_id（外部キーなし：モジュール外） ／ （外部キーなし：大量データ） |
| 選択肢 | 値：`GOOD`（良い）／`NORMAL`（普通）／`BAD`（悪い）（Javaの `enum` の名前） |

### IDのプレフィックス

| プレフィックス | 対象 | プレフィックス | 対象 |
|---|---|---|---|
| `USR` | アカウント（ユーザーID） | `CLS` | クラス |
| `PWH` | パスワード履歴 | `CHD` | 園児 |
| `RTK` | リフレッシュトークン | `PRT` | 保護者 |
| `NRS` | 園 | `PCH` | 保護者と園児の紐づけ |
| `ROL` | 役職 | `TDR` | 保育士連絡帳 |
| `TCH` | 保育士 | `TDI` | 保育士連絡帳の画像 |
| | | `PDR` | 保護者連絡帳 |
| | | `PDI` | 保護者連絡帳の画像 |
| | | `HNT` | 連携事項 |

### 選択肢の値（Javaの `enum`。DBには名前を文字で保存）

| 名前 | 値 |
|---|---|
| 園の状態 | `ACTIVE`（運営中）／`CLOSED`（閉園） |
| 性別 | `MALE`（男）／`FEMALE`（女）／`UNANSWERED`（回答しない） |
| 続柄 | `FATHER`（父）／`MOTHER`（母）／`GRANDFATHER`（祖父）／`GRANDMOTHER`（祖母）／`OTHER`（その他） |
| 機嫌 | `GOOD`（良い）／`NORMAL`（普通）／`BAD`（悪い） |
| 排便の状態 | `NORMAL`（普通）／`SOFT`（やわらかい）／`HARD`（かたい）／`DIARRHEA`（下痢） |
| 画像の種類 | `JPEG`／`PNG` |

---

# auth スキーマ（認証モジュール）

## credentials（アカウント）

ログインに必要な情報だけを持つ。保育士・保護者の業務情報は園運営モジュールが持つ（5-2-2）。

|No|論理名|物理名|型|PK|FK|NN|UQ|デフォルト値|備考|
|---:|---|---|---|:---:|:---:|:---:|:---:|---|---|
|1|ユーザーID|user_id|CHAR(29)|○||○|○||`USR`＋ULID|
|2|ログインID|email|VARCHAR(255)|||○|||メールアドレス|
|3|パスワードハッシュ|password_hash|VARCHAR(255)|||○|||Argon2id（ADR-30）|
|4|パスワード変更要否|must_change_password|BOOLEAN|||○||TRUE|初期パスワードの間はTRUE。パスワード変更・ログアウト以外のAPIを拒否する（ADR-57）|
|5|パスワード変更日時|password_changed_at|DATETIME|||||NULL||
|6|ログイン失敗回数|failed_login_count|TINYINT|||○||0|5回でロック。ログイン成功でリセット（ADR-39）|
|7|ロック日時|locked_at|DATETIME|||||NULL|ロック中ならNULL以外|
|8|ロック解除日時|unlocked_at|DATETIME|||||NULL||
|9|ロック解除者|unlocked_by|CHAR(29)|||||NULL|解除した園長・主任のユーザーID（ADR-55）|
|10|アカウント停止日時|suspended_at|DATETIME|||||NULL|アカウント停止中ならNULL以外|
|11|最終ログイン日時|last_login_at|DATETIME|||||NULL||
|12|作成日時|created_at|DATETIME|||○|||共通：監査項目|
|13|作成者|created_by|VARCHAR(29)|||○|||共通：監査項目|
|14|更新日時|updated_at|DATETIME|||○|||共通：監査項目|
|15|更新者|updated_by|VARCHAR(29)|||○|||共通：監査項目|
|16|削除日時|deleted_at|DATETIME|||||NULL|共通：論理削除|
|17|有効フラグ|active_flag|TINYINT（生成列）||||||共通：ユニーク制約用|

### 制約・インデックス

|種別|名前|カラム|参照先・備考|
|---|---|---|---|
|PK|PRIMARY|user_id||
|UQ|UQ_credentials_email|email, active_flag|削除されていないアカウントの中でメールアドレスを一意にする|

---

## password_histories（パスワード履歴）

過去5世代のパスワードの再利用を禁止する（ADR-30）。世代番号の大きい順に5件と比べる。

|No|論理名|物理名|型|PK|FK|NN|UQ|デフォルト値|備考|
|---:|---|---|---|:---:|:---:|:---:|:---:|---|---|
|1|パスワード履歴ID|password_history_id|CHAR(29)|○||○|○||`PWH`＋ULID|
|2|ユーザーID|user_id|CHAR(29)||○|○|||参照先：credentials.user_id|
|3|世代番号|generation_no|INT|||○|||ユーザーごとに1から連番。最新が最大。例：6まである場合、2〜6（現在のパスワードを含む5件）と一致しないことを確認する|
|4|パスワードハッシュ|password_hash|VARCHAR(255)|||○|||Argon2id|
|5|作成日時|created_at|DATETIME|||○|||共通：監査項目|
|6|作成者|created_by|VARCHAR(29)|||○|||共通：監査項目|
|7|更新日時|updated_at|DATETIME|||○|||共通：監査項目|
|8|更新者|updated_by|VARCHAR(29)|||○|||共通：監査項目|

### 制約・インデックス

|種別|名前|カラム|参照先・備考|
|---|---|---|---|
|PK|PRIMARY|password_history_id||
|FK|FK_password_histories_user|user_id|参照先：credentials.user_id|
|UQ|UQ_password_histories_generation|user_id, generation_no|同じユーザーの世代番号を一意にする。世代番号の大きい順に5件を取得するときにも使う|

---

## refresh_tokens（リフレッシュトークン）

Opaque Token。DBにはハッシュ値だけを保存する（ADR-35）。

|No|論理名|物理名|型|PK|FK|NN|UQ|デフォルト値|備考|
|---:|---|---|---|:---:|:---:|:---:|:---:|---|---|
|1|リフレッシュトークンID|refresh_token_id|CHAR(29)|○||○|○||`RTK`＋ULID|
|2|ユーザーID|user_id|CHAR(29)|||○|||参照先：credentials.user_id（外部キーなし：大量データ）|
|3|トークンハッシュ|token_hash|CHAR(64)|||○|○||SHA-256（16進数）|
|4|トークンファミリーID|family_id|CHAR(29)|||○|||1回のログインから交換で続いていく、同じユーザーのリフレッシュトークンのグループ。最初のトークンのIDを入れる（Rotationで一般的に使われる「トークンファミリー」の考え方）|
|5|有効期限|expires_at|DATETIME|||○|||最初のログインから12時間。トークンを交換しても同じ期限を引き継ぐ（延長しない）|
|6|無効化日時|revoked_at|DATETIME|||||NULL|交換・ログアウト・使い回しの検知で設定|
|7|作成日時|created_at|DATETIME|||○|||共通：監査項目|
|8|作成者|created_by|VARCHAR(29)|||○|||共通：監査項目|
|9|更新日時|updated_at|DATETIME|||○|||共通：監査項目|
|10|更新者|updated_by|VARCHAR(29)|||○|||共通：監査項目|

### 制約・インデックス

|種別|名前|カラム|参照先・備考|
|---|---|---|---|
|PK|PRIMARY|refresh_token_id||
|UQ|UQ_refresh_tokens_hash|token_hash|トークンの照合に使う|
|IDX|IDX_refresh_tokens_user|user_id|全端末のログアウト、全園退職時の無効化|
|IDX|IDX_refresh_tokens_family|family_id|使い回しを検知したときの一括無効化|

---

# nursery スキーマ（園運営モジュール）

## nurseries（園）

|No|論理名|物理名|型|PK|FK|NN|UQ|デフォルト値|備考|
|---:|---|---|---|:---:|:---:|:---:|:---:|---|---|
|1|園ID|nursery_id|CHAR(29)|○||○|○||`NRS`＋ULID|
|2|園名|nursery_name|VARCHAR(100)|||○||||
|3|園の状態|status|VARCHAR(20)|||○||'ACTIVE'|値：`ACTIVE`（運営中）／`CLOSED`（閉園）（ADR-48）|
|4|契約終了日|contract_end_date|DATE|||||NULL|この日の23:59:59（日本時間）まで利用可能（ADR-69）|
|5|連絡帳保存年数|diary_retention_years|TINYINT|||○||5|退園からの保存期間（ADR-44）|
|6|郵便番号|postal_code|VARCHAR(8)|||||||
|7|都道府県|prefecture|VARCHAR(20)|||||||
|8|市区町村|city|VARCHAR(100)|||||||
|9|住所|address|VARCHAR(255)|||||||
|10|電話番号|phone_number|VARCHAR(20)|||||||
|11|作成日時|created_at|DATETIME|||○|||共通：監査項目|
|12|作成者|created_by|VARCHAR(29)|||○|||共通：監査項目|
|13|更新日時|updated_at|DATETIME|||○|||共通：監査項目|
|14|更新者|updated_by|VARCHAR(29)|||○|||共通：監査項目|
|15|削除日時|deleted_at|DATETIME|||||NULL|共通：論理削除。閉園日時を入れる（ADR-46）|

### 制約・インデックス

|種別|名前|カラム|参照先・備考|
|---|---|---|---|
|PK|PRIMARY|nursery_id|園の検索は園IDだけで行うため、他のインデックスは付けない|

---

## nursery_roles（役職）

園ごとの役職。初期データは園長(4)・主任(3)・副主任(2)・一般(1)（3-4）。

|No|論理名|物理名|型|PK|FK|NN|UQ|デフォルト値|備考|
|---:|---|---|---|:---:|:---:|:---:|:---:|---|---|
|1|役職ID|role_id|CHAR(29)|○||○|○||`ROL`＋ULID|
|2|園ID|nursery_id|CHAR(29)||○|○|||参照先：nurseries.nursery_id|
|3|役職名|role_name|VARCHAR(50)|||○||||
|4|ランク|role_rank|TINYINT|||○|||大きいほど上位|
|5|作成日時|created_at|DATETIME|||○|||共通：監査項目|
|6|作成者|created_by|VARCHAR(29)|||○|||共通：監査項目|
|7|更新日時|updated_at|DATETIME|||○|||共通：監査項目|
|8|更新者|updated_by|VARCHAR(29)|||○|||共通：監査項目|
|9|削除日時|deleted_at|DATETIME|||||NULL|共通：論理削除|
|10|有効フラグ|active_flag|TINYINT（生成列）||||||共通：ユニーク制約用|

### 制約・インデックス

|種別|名前|カラム|参照先・備考|
|---|---|---|---|
|PK|PRIMARY|role_id||
|FK|FK_nursery_roles_nursery|nursery_id|参照先：nurseries.nursery_id|
|UQ|UQ_nursery_roles_rank|nursery_id, role_rank, active_flag|同じ園でランクを重複させない。園の役職一覧の取得にも使う|

---

## mst_permissions（権限マスタ）

全園共通の権限の一覧（表示用）。権限の種類はコードの `PermissionCode` で定義し、同じ値をFlywayで投入する（ADR-62論点5）。画面からは変更しないため、監査項目は持たない。

|No|論理名|物理名|型|PK|FK|NN|UQ|デフォルト値|備考|
|---:|---|---|---|:---:|:---:|:---:|:---:|---|---|
|1|権限コード|permission_code|VARCHAR(50)|○||○|○||例：`TEACHER_DIARY_EDIT_ALL`（3-3）|
|2|権限名|permission_name|VARCHAR(100)|||○|||画面の表示名|
|3|表示順|display_order|INT|||○|||権限の設定画面（追加要件）での並び順|

### 制約・インデックス

|種別|名前|カラム|参照先・備考|
|---|---|---|---|
|PK|PRIMARY|permission_code||

---

## role_permissions（役職権限）

役職に付けた権限。付け外しは行の追加・削除（物理削除）で表す。データ量は、園数×役職数×権限数（2.5万園×4役職×約15権限＝約150万行）を想定。

|No|論理名|物理名|型|PK|FK|NN|UQ|デフォルト値|備考|
|---:|---|---|---|:---:|:---:|:---:|:---:|---|---|
|1|役職ID|role_id|CHAR(29)|○|○|○|||参照先：nursery_roles.role_id|
|2|権限コード|permission_code|VARCHAR(50)|○|○|○|||参照先：mst_permissions.permission_code|
|3|作成日時|created_at|DATETIME|||○|||共通：監査項目|
|4|作成者|created_by|VARCHAR(29)|||○|||共通：監査項目|
|5|更新日時|updated_at|DATETIME|||○|||共通：監査項目|
|6|更新者|updated_by|VARCHAR(29)|||○|||共通：監査項目|

### 制約・インデックス

|種別|名前|カラム|参照先・備考|
|---|---|---|---|
|PK|PRIMARY|role_id, permission_code|役職の権限一覧の取得（全APIの確認のたびに使う。結果はRedisにキャッシュ）|
|FK|FK_role_permissions_role|role_id|参照先：nursery_roles.role_id（主キーの先頭と同じため、追加のインデックスは不要）|
|FK|FK_role_permissions_permission|permission_code|参照先：mst_permissions.permission_code|
|IDX|IDX_role_permissions_permission|permission_code|外部キーのためにMySQLが必要とする|

付け外しを誰がいつ行ったかは、監査ログに残す。
---

## classes（クラス）

|No|論理名|物理名|型|PK|FK|NN|UQ|デフォルト値|備考|
|---:|---|---|---|:---:|:---:|:---:|:---:|---|---|
|1|クラスID|class_id|CHAR(29)|○||○|○||`CLS`＋ULID|
|2|園ID|nursery_id|CHAR(29)||○|○|||参照先：nurseries.nursery_id|
|3|クラス名|class_name|VARCHAR(50)|||○|||例：ひよこ組|
|4|表示順|display_order|INT|||○||1|画面でクラスを並べる順番（例：0歳児クラス→5歳児クラスの年齢順）。クラス名の50音順では年齢順にならないため|
|5|作成日時|created_at|DATETIME|||○|||共通：監査項目|
|6|作成者|created_by|VARCHAR(29)|||○|||共通：監査項目|
|7|更新日時|updated_at|DATETIME|||○|||共通：監査項目|
|8|更新者|updated_by|VARCHAR(29)|||○|||共通：監査項目|
|9|削除日時|deleted_at|DATETIME|||||NULL|共通：論理削除|

### 制約・インデックス

|種別|名前|カラム|参照先・備考|
|---|---|---|---|
|PK|PRIMARY|class_id||
|FK|FK_classes_nursery|nursery_id|参照先：nurseries.nursery_id|
|IDX|IDX_classes_nursery|nursery_id, display_order|園のクラス一覧を表示順で取得|

---

## teachers（保育士）

園ごとの所属。2つの園を兼務する場合は、同じユーザーIDで2行になる。

|No|論理名|物理名|型|PK|FK|NN|UQ|デフォルト値|備考|
|---:|---|---|---|:---:|:---:|:---:|:---:|---|---|
|1|保育士ID|teacher_id|CHAR(29)|○||○|○||`TCH`＋ULID|
|2|ユーザーID|user_id|CHAR(29)|||○|||参照先：auth.credentials.user_id（外部キーなし：モジュール外）|
|3|園ID|nursery_id|CHAR(29)||○|○|||参照先：nurseries.nursery_id|
|4|役職ID|role_id|CHAR(29)||○|○|||参照先：nursery_roles.role_id|
|5|姓|last_name|VARCHAR(50)|||○||||
|6|名|first_name|VARCHAR(50)|||○||||
|7|メールアドレス|email|VARCHAR(255)|||○|||業務情報。ログインIDと同時に変更する（5-2-2）|
|8|所属開始日|started_on|DATE|||○||||
|9|所属終了日|ended_on|DATE|||||NULL|退職日。事前に入力できる（ADR-43）|
|10|作成日時|created_at|DATETIME|||○|||共通：監査項目|
|11|作成者|created_by|VARCHAR(29)|||○|||共通：監査項目|
|12|更新日時|updated_at|DATETIME|||○|||共通：監査項目|
|13|更新者|updated_by|VARCHAR(29)|||○|||共通：監査項目|
|14|削除日時|deleted_at|DATETIME|||||NULL|共通：論理削除|
|15|有効フラグ|active_flag|TINYINT（生成列）||||||共通：ユニーク制約用|

### 制約・インデックス

|種別|名前|カラム|参照先・備考|
|---|---|---|---|
|PK|PRIMARY|teacher_id||
|FK|FK_teachers_nursery|nursery_id|参照先：nurseries.nursery_id|
|FK|FK_teachers_role|role_id|参照先：nursery_roles.role_id|
|UQ|UQ_teachers_user_nursery|user_id, nursery_id, active_flag|同じ園に同じ人は1行。ユーザーIDからの検索（園の選択・所属の確認）にも使う|

---

## class_assignments（担当）

保育士が担当するクラス（複数可。ADR-62論点3）。担当の解除は行の削除（物理削除）で表す。

|No|論理名|物理名|型|PK|FK|NN|UQ|デフォルト値|備考|
|---:|---|---|---|:---:|:---:|:---:|:---:|---|---|
|1|保育士ID|teacher_id|CHAR(29)|○|○|○|||参照先：teachers.teacher_id|
|2|クラスID|class_id|CHAR(29)|○|○|○|||参照先：classes.class_id|
|3|作成日時|created_at|DATETIME|||○|||共通：監査項目|
|4|作成者|created_by|VARCHAR(29)|||○|||共通：監査項目|
|5|更新日時|updated_at|DATETIME|||○|||共通：監査項目|
|6|更新者|updated_by|VARCHAR(29)|||○|||共通：監査項目|

### 制約・インデックス

|種別|名前|カラム|参照先・備考|
|---|---|---|---|
|PK|PRIMARY|teacher_id, class_id|保育士の担当クラスの取得|
|FK|FK_class_assignments_teacher|teacher_id|参照先：teachers.teacher_id（主キーの先頭と同じため、追加のインデックスは不要）|
|FK|FK_class_assignments_class|class_id|参照先：classes.class_id|
|IDX|IDX_class_assignments_class|class_id|外部キーのためにMySQLが必要とする|

担当の付け外しを誰がいつ行ったかは、監査ログに残す。
---

## children（園児）

|No|論理名|物理名|型|PK|FK|NN|UQ|デフォルト値|備考|
|---:|---|---|---|:---:|:---:|:---:|:---:|---|---|
|1|園児ID|child_id|CHAR(29)|○||○|○||`CHD`＋ULID|
|2|園ID|nursery_id|CHAR(29)||○|○|||参照先：nurseries.nursery_id|
|3|クラスID|class_id|CHAR(29)||○|○|||参照先：classes.class_id|
|4|姓|last_name|VARCHAR(50)|||○||||
|5|名|first_name|VARCHAR(50)|||○||||
|6|生年月日|birthday|DATE|||○||||
|7|性別|gender|VARCHAR(20)|||○|||値：`MALE`（男）／`FEMALE`（女）／`UNANSWERED`（回答しない）|
|8|入園日|enrolled_on|DATE|||○||||
|9|退園日|left_on|DATE|||||NULL|所属期間で管理（ADR-43）|
|10|メイン保護者ID|main_parent_id|CHAR(29)||○|○|||参照先：parents.parent_id。必須。園児の登録時に、保護者・園児・紐づけを1つのトランザクションで作る（ADR-12、ADR-86）|
|11|作成日時|created_at|DATETIME|||○|||共通：監査項目|
|12|作成者|created_by|VARCHAR(29)|||○|||共通：監査項目|
|13|更新日時|updated_at|DATETIME|||○|||共通：監査項目|
|14|更新者|updated_by|VARCHAR(29)|||○|||共通：監査項目|
|15|削除日時|deleted_at|DATETIME|||||NULL|共通：論理削除|

### 制約・インデックス

|種別|名前|カラム|参照先・備考|
|---|---|---|---|
|PK|PRIMARY|child_id||
|FK|FK_children_nursery|nursery_id|参照先：nurseries.nursery_id|
|FK|FK_children_class|class_id|参照先：classes.class_id|
|FK|FK_children_main_parent|main_parent_id|参照先：parents.parent_id|
|IDX|IDX_children_nursery_class|nursery_id, class_id|閲覧範囲（ViewableScope）の作成：担当クラスの園児、園の全園児|
|IDX|IDX_children_main_parent|main_parent_id|外部キーのためにMySQLが必要とする|

---

## parents（保護者）

公開版では保護者はログインしない（テストデータ）。保護者のログインは追加要件。

|No|論理名|物理名|型|PK|FK|NN|UQ|デフォルト値|備考|
|---:|---|---|---|:---:|:---:|:---:|:---:|---|---|
|1|保護者ID|parent_id|CHAR(29)|○||○|○||`PRT`＋ULID|
|2|ユーザーID|user_id|CHAR(29)|||||NULL|参照先：auth.credentials.user_id（外部キーなし：モジュール外）。保護者アプリ（追加要件）で使う|
|3|姓|last_name|VARCHAR(50)|||○||||
|4|名|first_name|VARCHAR(50)|||○||||
|5|メールアドレス|email|VARCHAR(255)|||||NULL|業務情報|
|6|作成日時|created_at|DATETIME|||○|||共通：監査項目|
|7|作成者|created_by|VARCHAR(29)|||○|||共通：監査項目|
|8|更新日時|updated_at|DATETIME|||○|||共通：監査項目|
|9|更新者|updated_by|VARCHAR(29)|||○|||共通：監査項目|
|10|削除日時|deleted_at|DATETIME|||||NULL|共通：論理削除|

### 制約・インデックス

|種別|名前|カラム|参照先・備考|
|---|---|---|---|
|PK|PRIMARY|parent_id||

---

## parent_children（保護者園児紐づけ）

|No|論理名|物理名|型|PK|FK|NN|UQ|デフォルト値|備考|
|---:|---|---|---|:---:|:---:|:---:|:---:|---|---|
|1|紐づけID|parent_child_id|CHAR(29)|○||○|○||`PCH`＋ULID|
|2|保護者ID|parent_id|CHAR(29)||○|○|||参照先：parents.parent_id|
|3|園児ID|child_id|CHAR(29)||○|○|||参照先：children.child_id|
|4|続柄|relationship|VARCHAR(20)|||○|||値：`FATHER`（父）／`MOTHER`（母）／`GRANDFATHER`（祖父）／`GRANDMOTHER`（祖母）／`OTHER`（その他）|
|5|紐づけ開始日|started_on|DATE|||○||||
|6|紐づけ終了日|ended_on|DATE|||||NULL|紐づけ解除・退園（ADR-13、ADR-43）|
|7|作成日時|created_at|DATETIME|||○|||共通：監査項目|
|8|作成者|created_by|VARCHAR(29)|||○|||共通：監査項目|
|9|更新日時|updated_at|DATETIME|||○|||共通：監査項目|
|10|更新者|updated_by|VARCHAR(29)|||○|||共通：監査項目|
|11|削除日時|deleted_at|DATETIME|||||NULL|共通：論理削除|
|12|有効フラグ|active_flag|TINYINT（生成列）||||||共通：ユニーク制約用|

### 制約・インデックス

|種別|名前|カラム|参照先・備考|
|---|---|---|---|
|PK|PRIMARY|parent_child_id||
|FK|FK_parent_children_parent|parent_id|参照先：parents.parent_id|
|FK|FK_parent_children_child|child_id|参照先：children.child_id|
|UQ|UQ_parent_children|parent_id, child_id, active_flag|同じ保護者と園児の紐づけは1行|
|IDX|IDX_parent_children_child|child_id|園児の保護者一覧の取得、外部キーのため|

保護者ごとの権限（園児情報の閲覧など）は、保護者アプリ（追加要件）と合わせて追加する。
---

# diary スキーマ（連絡帳モジュール）

## teacher_diaries（保育士連絡帳）

項目の並びは画面（保育士作成）の順。データ量は多い（250万園児×年間約240日）が、書き込みは1園児1日1件程度で、検索（読み込み）の方がはるかに多い。

|No|論理名|物理名|型|PK|FK|NN|UQ|デフォルト値|備考|
|---:|---|---|---|:---:|:---:|:---:|:---:|---|---|
|1|保育士連絡帳ID|teacher_diary_id|CHAR(29)|○||○|○||`TDR`＋ULID|
|2|園ID|nursery_id|CHAR(29)|||○|||参照先：nursery.nurseries.nursery_id（外部キーなし：モジュール外）。閲覧範囲の条件に使う|
|3|園児ID|child_id|CHAR(29)|||○|||参照先：nursery.children.child_id（外部キーなし：モジュール外）|
|4|作成保育士ID|author_teacher_id|CHAR(29)|||○|||参照先：nursery.teachers.teacher_id（外部キーなし：モジュール外）。自分の連絡帳か（`OWN`）の判定に使う。監査項目の作成者（ユーザーID）とは別に持つ（ADR-87）|
|5|作成保育士名|author_name|VARCHAR(100)|||○|||スナップショット（ADR-45）|
|6|園児名|child_name|VARCHAR(100)|||○|||スナップショット|
|7|クラス名|class_name|VARCHAR(50)|||○|||スナップショット|
|8|対象日|target_date|DATE|||○|||画面：日付|
|9|体調（午前）|condition_am|VARCHAR(200)||||||画面：【体調】午前|
|10|体調（午後）|condition_pm|VARCHAR(200)||||||画面：【体調】午後|
|11|体温|temperature|DECIMAL(3,1)||||||画面：【体温】体温。34.0〜43.0（Value Objectで確認）|
|12|体温計測時刻|temperature_measured_at|TIME||||||画面：【体温】計測時刻|
|13|機嫌（午前）|mood_am|VARCHAR(20)||||||画面：【機嫌】午前。値：`GOOD`（良い）／`NORMAL`（普通）／`BAD`（悪い）|
|14|機嫌（午後）|mood_pm|VARCHAR(20)||||||画面：【機嫌】午後。値：`GOOD`（良い）／`NORMAL`（普通）／`BAD`（悪い）|
|15|排便回数（午前）|bowel_count_am|TINYINT||||||画面：【排便】午前回数。0以上|
|16|排便状態（午前）|bowel_status_am|VARCHAR(20)||||||画面：【排便】午前状態。値：`NORMAL`（普通）／`SOFT`（やわらかい）／`HARD`（かたい）／`DIARRHEA`（下痢）|
|17|排便回数（午後）|bowel_count_pm|TINYINT||||||画面：【排便】午後回数。0以上|
|18|排便状態（午後）|bowel_status_pm|VARCHAR(20)||||||画面：【排便】午後状態。値：`NORMAL`（普通）／`SOFT`（やわらかい）／`HARD`（かたい）／`DIARRHEA`（下痢）|
|19|給食|lunch|VARCHAR(200)||||||画面：【食事】昼|
|20|おやつ|snack|VARCHAR(200)||||||画面：【食事】おやつ|
|21|午睡開始時刻|nap_start_at|TIME||||||画面：【睡眠】入眠|
|22|午睡終了時刻|nap_end_at|TIME||||||画面：【睡眠】起床|
|23|保育士のコメント|teacher_comment|VARCHAR(1000)||||||画面：【コメント】保育士のコメント|
|24|バージョン|version|INT|||○||1|楽観的ロック用。作成時1、更新のたびに+1。更新時に画面で読み込んだ値と一致しなければ409（他の人が先に更新した）|
|25|作成日時|created_at|DATETIME|||○|||共通：監査項目|
|26|作成者|created_by|VARCHAR(29)|||○|||共通：監査項目|
|27|更新日時|updated_at|DATETIME|||○|||共通：監査項目|
|28|更新者|updated_by|VARCHAR(29)|||○|||共通：監査項目|
|29|削除日時|deleted_at|DATETIME|||||NULL|共通：論理削除|
|30|有効フラグ|active_flag|TINYINT（生成列）||||||共通：ユニーク制約用|

### 制約・インデックス

|種別|名前|カラム|参照先・備考|
|---|---|---|---|
|PK|PRIMARY|teacher_diary_id|詳細の取得|
|UQ|UQ_teacher_diaries_child_date|child_id, target_date, active_flag|1園児1日1件（削除済みは除く。ADR-80）|
|IDX|IDX_teacher_diaries_search|nursery_id, child_id, target_date|検索・一覧：`WHERE nursery_id = ? AND child_id IN (...) AND target_date BETWEEN ? AND ? AND deleted_at IS NULL`（3-6）|

必須の入力項目（体調・機嫌など）は、画面の設計時（10/30まで）に決める。文字数の上限は、短い項目200文字・コメント1,000文字（ADR-85）。
---

## teacher_diary_images（保育士連絡帳画像）

1つの保育士連絡帳につき最大5枚。枚数の上限は連絡帳の集約（TeacherDiary）が守る。位置情報を除去した画像をS3に保存する（ADR-56）。閲覧URLは30秒の署名付きURLを毎回作るため保存しない（8-10）。

|No|論理名|物理名|型|PK|FK|NN|UQ|デフォルト値|備考|
|---:|---|---|---|:---:|:---:|:---:|:---:|---|---|
|1|保育士連絡帳画像ID|teacher_diary_image_id|CHAR(29)|○||○|○||`TDI`＋ULID|
|2|保育士連絡帳ID|teacher_diary_id|CHAR(29)|||○|||参照先：teacher_diaries.teacher_diary_id（外部キーなし：大量データ。同じ集約としてまとめて保存・削除する）|
|3|S3キー|s3_object_key|VARCHAR(255)|||○|○||S3の保存場所。IDで作る（元のファイル名は使わない）|
|4|画像種類|content_type|VARCHAR(20)|||○|||値：`JPEG`／`PNG`|
|5|ファイルサイズ|file_size_bytes|INT|||○|||画像1枚の大きさ（バイト）。1枚の上限は10MB（ADR-85）|
|6|表示順|display_order|TINYINT|||○||1|連絡帳の中での画像の並び順（1〜5）|
|7|作成日時|created_at|DATETIME|||○|||共通：監査項目|
|8|作成者|created_by|VARCHAR(29)|||○|||共通：監査項目|
|9|更新日時|updated_at|DATETIME|||○|||共通：監査項目|
|10|更新者|updated_by|VARCHAR(29)|||○|||共通：監査項目|
|11|削除日時|deleted_at|DATETIME|||||NULL|共通：論理削除|

### 制約・インデックス

|種別|名前|カラム|参照先・備考|
|---|---|---|---|
|PK|PRIMARY|teacher_diary_image_id||
|UQ|UQ_teacher_diary_images_key|s3_object_key||
|IDX|IDX_teacher_diary_images_diary|teacher_diary_id, display_order|連絡帳の画像を表示順で取得|

---

## parent_diaries（保護者連絡帳）

項目の並びは画面（保護者作成）の順。公開版ではテストデータで作成する。保育士は閲覧・画像削除・削除のみ（文章の編集不可）。

|No|論理名|物理名|型|PK|FK|NN|UQ|デフォルト値|備考|
|---:|---|---|---|:---:|:---:|:---:|:---:|---|---|
|1|保護者連絡帳ID|parent_diary_id|CHAR(29)|○||○|○||`PDR`＋ULID|
|2|園ID|nursery_id|CHAR(29)|||○|||参照先：nursery.nurseries.nursery_id（外部キーなし：モジュール外）。閲覧範囲の条件に使う|
|3|園児ID|child_id|CHAR(29)|||○|||参照先：nursery.children.child_id（外部キーなし：モジュール外）|
|4|作成保護者ID|author_parent_id|CHAR(29)|||○|||参照先：nursery.parents.parent_id（外部キーなし：モジュール外）。自分の連絡帳かの判定に使う（ADR-87）|
|5|作成保護者名|author_name|VARCHAR(100)|||○|||スナップショット（ADR-45）|
|6|園児名|child_name|VARCHAR(100)|||○|||スナップショット|
|7|クラス名|class_name|VARCHAR(50)|||○|||スナップショット|
|8|対象日|target_date|DATE|||○|||画面：日付|
|9|体調（夜）|condition_night|VARCHAR(200)||||||画面：【体調】夜|
|10|体調（朝）|condition_morning|VARCHAR(200)||||||画面：【体調】朝|
|11|体温|temperature|DECIMAL(3,1)||||||画面：【体温】体温。34.0〜43.0（Value Objectで確認）|
|12|体温計測時刻|temperature_measured_at|TIME||||||画面：【体温】計測時刻|
|13|機嫌（夜）|mood_night|VARCHAR(20)||||||画面：【機嫌】夜。値：`GOOD`（良い）／`NORMAL`（普通）／`BAD`（悪い）|
|14|機嫌（朝）|mood_morning|VARCHAR(20)||||||画面：【機嫌】朝。値：`GOOD`（良い）／`NORMAL`（普通）／`BAD`（悪い）|
|15|排便回数（夜）|bowel_count_night|TINYINT||||||画面：【排便】夜回数。0以上|
|16|排便状態（夜）|bowel_status_night|VARCHAR(20)||||||画面：【排便】夜状態。値：`NORMAL`（普通）／`SOFT`（やわらかい）／`HARD`（かたい）／`DIARRHEA`（下痢）|
|17|排便回数（朝）|bowel_count_morning|TINYINT||||||画面：【排便】朝回数。0以上|
|18|排便状態（朝）|bowel_status_morning|VARCHAR(20)||||||画面：【排便】朝状態。値：`NORMAL`（普通）／`SOFT`（やわらかい）／`HARD`（かたい）／`DIARRHEA`（下痢）|
|19|夕食|dinner|VARCHAR(200)||||||画面：【食事】夜|
|20|朝食|breakfast|VARCHAR(200)||||||画面：【食事】朝|
|21|就寝時刻|sleep_start_at|TIME||||||画面：【睡眠】入眠|
|22|起床時刻|sleep_end_at|TIME||||||画面：【睡眠】起床|
|23|保護者のコメント|parent_comment|VARCHAR(1000)||||||画面：【コメント】保護者のコメント|
|24|バージョン|version|INT|||○||1|楽観的ロック用。作成時1、更新のたびに+1。更新時に画面で読み込んだ値と一致しなければ409（他の人が先に更新した）|
|25|作成日時|created_at|DATETIME|||○|||共通：監査項目|
|26|作成者|created_by|VARCHAR(29)|||○|||共通：監査項目|
|27|更新日時|updated_at|DATETIME|||○|||共通：監査項目|
|28|更新者|updated_by|VARCHAR(29)|||○|||共通：監査項目|
|29|削除日時|deleted_at|DATETIME|||||NULL|共通：論理削除|
|30|有効フラグ|active_flag|TINYINT（生成列）||||||共通：ユニーク制約用|

### 制約・インデックス

|種別|名前|カラム|参照先・備考|
|---|---|---|---|
|PK|PRIMARY|parent_diary_id|詳細の取得|
|UQ|UQ_parent_diaries_child_date|child_id, target_date, active_flag|1園児1日1件（削除済みは除く。ADR-80）|
|IDX|IDX_parent_diaries_search|nursery_id, child_id, target_date|検索・一覧（3-6）|

---

## parent_diary_images（保護者連絡帳画像）

1つの保護者連絡帳につき最大5枚。枚数の上限は連絡帳の集約（ParentDiary）が守る。

|No|論理名|物理名|型|PK|FK|NN|UQ|デフォルト値|備考|
|---:|---|---|---|:---:|:---:|:---:|:---:|---|---|
|1|保護者連絡帳画像ID|parent_diary_image_id|CHAR(29)|○||○|○||`PDI`＋ULID|
|2|保護者連絡帳ID|parent_diary_id|CHAR(29)|||○|||参照先：parent_diaries.parent_diary_id（外部キーなし：大量データ。同じ集約としてまとめて保存・削除する）|
|3|S3キー|s3_object_key|VARCHAR(255)|||○|○||S3の保存場所。IDで作る（元のファイル名は使わない）|
|4|画像種類|content_type|VARCHAR(20)|||○|||値：`JPEG`／`PNG`|
|5|ファイルサイズ|file_size_bytes|INT|||○|||画像1枚の大きさ（バイト）。1枚の上限は10MB（ADR-85）|
|6|表示順|display_order|TINYINT|||○||1|連絡帳の中での画像の並び順（1〜5）|
|7|作成日時|created_at|DATETIME|||○|||共通：監査項目|
|8|作成者|created_by|VARCHAR(29)|||○|||共通：監査項目|
|9|更新日時|updated_at|DATETIME|||○|||共通：監査項目|
|10|更新者|updated_by|VARCHAR(29)|||○|||共通：監査項目|
|11|削除日時|deleted_at|DATETIME|||||NULL|共通：論理削除|

### 制約・インデックス

|種別|名前|カラム|参照先・備考|
|---|---|---|---|
|PK|PRIMARY|parent_diary_image_id||
|UQ|UQ_parent_diary_images_key|s3_object_key||
|IDX|IDX_parent_diary_images_diary|parent_diary_id, display_order|連絡帳の画像を表示順で取得|

---

## handover_notes（連携事項）

保育士同士の申し送り（例：「園庭で遊んでいてけがをしました。保護者への連携をお願いします」）。公開版ではテストデータで作成し、トップに表示するだけ。作成・編集・削除の画面とAPIは追加要件（ADR-93）。閲覧できるのは、対象の園児を見られる人（`ChildScope`）だけ。

|No|論理名|物理名|型|PK|FK|NN|UQ|デフォルト値|備考|
|---:|---|---|---|:---:|:---:|:---:|:---:|---|---|
|1|連携事項ID|handover_note_id|CHAR(29)|○||○|○||`HNT`＋ULID|
|2|園ID|nursery_id|CHAR(29)|||○|||参照先：nursery.nurseries.nursery_id（外部キーなし：モジュール外）。閲覧範囲の条件に使う|
|3|園児ID|child_id|CHAR(29)|||○|||参照先：nursery.children.child_id（外部キーなし：モジュール外）|
|4|作成保育士ID|author_teacher_id|CHAR(29)|||○|||参照先：nursery.teachers.teacher_id（外部キーなし：モジュール外）。編集・削除（追加要件）で本人の判定に使う|
|5|作成保育士名|author_name|VARCHAR(100)|||○|||スナップショット（ADR-45）|
|6|園児名|child_name|VARCHAR(100)|||○|||スナップショット|
|7|本文|content|VARCHAR(500)|||○||||
|8|作成日時|created_at|DATETIME(3)|||○|||共通：監査項目。**このテーブルだけミリ秒まで持ち、作成の新しい順に厳密に並べる**（ADR-81の例外。ADR-93）|
|9|作成者|created_by|VARCHAR(29)|||○|||共通：監査項目|
|10|更新日時|updated_at|DATETIME|||○|||共通：監査項目|
|11|更新者|updated_by|VARCHAR(29)|||○|||共通：監査項目|
|12|削除日時|deleted_at|DATETIME||||||NULL|共通：論理削除|

### 制約・インデックス

|種別|名前|カラム|参照先・備考|
|---|---|---|---|
|PK|PRIMARY|handover_note_id||
|IDX|IDX_handover_notes_top|nursery_id, child_id, created_at|トップ：見てよい園児の直近7日間の連携事項を、作成の新しい順に取得|

---

# 公開版で作らないテーブル（追加要件）

| テーブル | 理由 |
|---|---|
| 保育士連絡帳・保護者連絡帳の編集履歴 | 編集履歴は追加要件（ADR-54） |
| 既読サマリー | 既読は追加要件（ADR-54） |
| 下書き | 下書きは追加要件（ADR-38） |
| 保護者のパスワード・リフレッシュトークン | 保護者のログインは追加要件。保護者のアカウントも `credentials` を使う |

# 第2版の変更点

| # | 変更 | 理由 |
|---|---|---|
| 1 | 日時を `DATETIME(3)` から `DATETIME`（秒まで）に | ミリ秒までの順番が必要な箇所がない（パスワード履歴は世代番号、作成順はULIDで分かる）（論点X） |
| 2 | 選択肢をJavaの `enum`＋DBは文字（`VARCHAR(20)`）に。値を一覧にまとめた | 変更容易性（値の追加・並べ替えでDBを変えずに済む）（第3版） |
| 3 | `permissions` を `mst_permissions`（権限マスタ）に。監査項目を外した | Flywayで投入する全園共通の固定データのため |
| 4 | 論理名を日本語にそろえた（例：User ID → ユーザーID） | 論理名と物理名の区別 |
| 5 | 備考の書き方をそろえた（参照先、外部キーなしの理由、選択肢の値、共通の項目） | 粒度の統一 |
| 6 | 全テーブルに「制約・インデックス」の表（PK・FK・UQ・IDX）を付けた | 粒度の統一 |
| 7 | `failed_login_count` を `TINYINT` に | 5回でロックするため、小さい値で足りる |
| 8 | `password_histories` に世代番号を追加 | 世代番号の大きい順に5件と比べる |
| 9 | `role_permissions`・`class_assignments` に更新日時・更新者を追加 | 監査項目の統一 |
| 10 | 連絡帳の項目を画面の項目と順番に合わせた。保護者連絡帳は夜・朝に変更。機嫌の列を追加・整理。コメントの名前を修正 | 画面の項目との一致 |
| 11 | 連絡帳のコメントを `TEXT` から `VARCHAR`（200文字・1,000文字）に | 上限がないと表示の崩れや大量の入力を防げない（ADR-85） |
| 12 | 画像の表示順を `TINYINT` に。最大5枚を明記 | 1連絡帳あたり最大5枚 |
| 13 | `children.main_parent_id` を必須に | メイン保護者がいないと、将来の保護者アプリで園児の閲覧権限を設定する人がいなくなる（第3版、論点Z） |
| 14 | 園コード（`nursery_code`）は持たない | 用途が出たら追加する（ADR-49）。MySQL 8.0の列の追加はINSTANT方式で一瞬で終わる。追加要件に記載（第3版、論点Y） |

# 追加要件（テーブルに関係するもの）

| 内容 | 備考 |
|---|---|
| 園コード（`nurseries.nursery_code`） | 契約時の入力、問い合わせ時の本人確認など、人が使う用途が出たら追加（ADR-49） |
| 連携事項の作成・編集・削除 | 公開版は表示だけ（テストデータ）。時間が余れば公開前でも着手（ADR-93） |
| 15 | 連携事項（`handover_notes`）を追加。作成日時だけ `DATETIME(3)` | トップに表示（公開版はテストデータ）。作成順に厳密に並べるため（ADR-93） |
