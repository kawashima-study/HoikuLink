# ER図（公開版）

- 作成日：2026-10-07
- 元にしたテーブル定義：table-design.md（修正版）
- 実線：外部キーあり（同じモジュールの中の、データが少ないテーブル）
- 点線：外部キーなし。①モジュールをまたぐ関係（論点N）、②大量データ・書き込みが多いテーブル（連絡帳の画像、Refresh Token。P2）。IDで参照し、集約とアプリで整合性を守る
- 監査項目（created_at など）と論理削除の列は省略

## 全体

```mermaid
erDiagram
    %% ===== auth スキーマ =====
    credentials ||--o{ password_histories : "パスワード履歴"
    credentials ||..o{ refresh_tokens : "Refresh Token"

    %% ===== nursery スキーマ =====
    nurseries ||--o{ nursery_roles : "役職"
    nursery_roles ||--o{ role_permissions : "権限の付与"
    mst_permissions ||--o{ role_permissions : "権限"
    nurseries ||--o{ classes : "クラス"
    nurseries ||--o{ teachers : "所属"
    nursery_roles ||--o{ teachers : "役職"
    teachers ||--o{ class_assignments : "担当"
    classes ||--o{ class_assignments : "担当"
    nurseries ||--o{ children : "在籍"
    classes ||--o{ children : "所属クラス"
    parents ||--o{ parent_children : "紐づけ"
    children ||--o{ parent_children : "紐づけ"
    parents |o--o{ children : "メイン保護者"

    %% ===== diary スキーマ =====
    teacher_diaries ||..o{ teacher_diary_images : "画像"
    parent_diaries ||..o{ parent_diary_images : "画像"

    %% ===== モジュールをまたぐ参照（外部キーなし） =====
    credentials ||..o{ teachers : "user_id"
    credentials |o..o| parents : "user_id（将来）"
    children ||..o{ teacher_diaries : "child_id"
    children ||..o{ parent_diaries : "child_id"
    teachers ||..o{ teacher_diaries : "author_teacher_id"
    parents ||..o{ parent_diaries : "author_parent_id"
    children ||..o{ handover_notes : "child_id"
    teachers ||..o{ handover_notes : "author_teacher_id"

    credentials {
        CHAR29 user_id PK
        VARCHAR email
        VARCHAR password_hash
        BOOLEAN must_change_password
        INT failed_login_count
        DATETIME locked_at
        DATETIME unlocked_at
        CHAR29 unlocked_by
        DATETIME suspended_at
    }
    password_histories {
        CHAR29 password_history_id PK
        CHAR29 user_id FK
        INT generation_no
        VARCHAR password_hash
    }
    refresh_tokens {
        CHAR29 refresh_token_id PK
        CHAR29 user_id
        CHAR64 token_hash UK
        CHAR29 family_id
        DATETIME expires_at
        DATETIME revoked_at
    }
    nurseries {
        CHAR29 nursery_id PK
        VARCHAR nursery_name
        VARCHAR status
        DATE contract_end_date
        TINYINT diary_retention_years
    }
    nursery_roles {
        CHAR29 role_id PK
        CHAR29 nursery_id FK
        VARCHAR role_name
        TINYINT role_rank
    }
    mst_permissions {
        VARCHAR permission_code PK
        VARCHAR permission_name
    }
    role_permissions {
        CHAR29 role_id PK
        VARCHAR permission_code PK
    }
    classes {
        CHAR29 class_id PK
        CHAR29 nursery_id FK
        VARCHAR class_name
    }
    teachers {
        CHAR29 teacher_id PK
        CHAR29 user_id
        CHAR29 nursery_id FK
        CHAR29 role_id FK
        DATE started_on
        DATE ended_on
    }
    class_assignments {
        CHAR29 teacher_id PK
        CHAR29 class_id PK
    }
    children {
        CHAR29 child_id PK
        CHAR29 nursery_id FK
        CHAR29 class_id FK
        DATE enrolled_on
        DATE left_on
        CHAR29 main_parent_id FK
    }
    parents {
        CHAR29 parent_id PK
        CHAR29 user_id
    }
    parent_children {
        CHAR29 parent_child_id PK
        CHAR29 parent_id FK
        CHAR29 child_id FK
        DATE started_on
        DATE ended_on
    }
    teacher_diaries {
        CHAR29 teacher_diary_id PK
        CHAR29 nursery_id
        CHAR29 child_id
        DATE target_date
        CHAR29 author_teacher_id
        INT version
    }
    teacher_diary_images {
        CHAR29 teacher_diary_image_id PK
        CHAR29 teacher_diary_id
        VARCHAR s3_object_key UK
    }
    parent_diaries {
        CHAR29 parent_diary_id PK
        CHAR29 nursery_id
        CHAR29 child_id
        DATE target_date
        CHAR29 author_parent_id
        INT version
    }
    handover_notes {
        CHAR29 handover_note_id PK
        CHAR29 nursery_id
        CHAR29 child_id
        CHAR29 author_teacher_id
        VARCHAR content
        DATETIME3 created_at
    }
    parent_diary_images {
        CHAR29 parent_diary_image_id PK
        CHAR29 parent_diary_id
        VARCHAR s3_object_key UK
    }
```

## モジュールごとの見方

| モジュール（スキーマ） | テーブル | 集約（2-8-2） |
|---|---|---|
| auth | credentials、password_histories | UserAccount |
| auth | refresh_tokens | RefreshToken |
| nursery | nurseries | Nursery |
| nursery | nursery_roles、role_permissions（mst_permissions は権限マスタ） | NurseryRole |
| nursery | teachers、class_assignments | Teacher |
| nursery | classes | ClassRoom |
| nursery | children、parent_children | Child |
| nursery | parents | Parent |
| diary | teacher_diaries、teacher_diary_images | TeacherDiary |
| diary | parent_diaries、parent_diary_images | ParentDiary |
| diary | handover_notes | HandoverNote（公開版は表示だけ） |
