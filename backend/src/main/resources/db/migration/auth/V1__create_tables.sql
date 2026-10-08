-- 認証モジュールのテーブル
-- 日時はUTC（DATETIME、秒まで）。論理削除とユニーク制約は active_flag（生成列）で両立する

-- アカウント
CREATE TABLE `auth`.`credentials` (
                                      `user_id`              CHAR(29)     NOT NULL,
                                      `email`                VARCHAR(255) NOT NULL,
                                      `password_hash`        VARCHAR(255) NOT NULL,
                                      `must_change_password` BOOLEAN      NOT NULL DEFAULT TRUE,
                                      `password_changed_at`  DATETIME     NULL,
                                      `failed_login_count`   TINYINT      NOT NULL DEFAULT 0,
                                      `locked_at`            DATETIME     NULL,
                                      `unlocked_at`          DATETIME     NULL,
                                      `unlocked_by`          CHAR(29)     NULL,
                                      `suspended_at`         DATETIME     NULL,
                                      `last_login_at`        DATETIME     NULL,
                                      `created_at`           DATETIME     NOT NULL,
                                      `created_by`           VARCHAR(29)  NOT NULL,
                                      `updated_at`           DATETIME     NOT NULL,
                                      `updated_by`           VARCHAR(29)  NOT NULL,
                                      `deleted_at`           DATETIME     NULL,
                                      `active_flag`          TINYINT
                                          GENERATED ALWAYS AS (CASE WHEN `deleted_at` IS NULL THEN 1 ELSE NULL END) STORED,
                                      CONSTRAINT `PK_credentials` PRIMARY KEY (`user_id`),
                                      CONSTRAINT `UQ_credentials_email` UNIQUE (`email`, `active_flag`)
);

-- パスワード履歴（世代番号の大きい順に5件と比べる。）
CREATE TABLE `auth`.`password_histories` (
                                             `password_history_id` CHAR(29)     NOT NULL,
                                             `user_id`             CHAR(29)     NOT NULL,
                                             `generation_no`       INT          NOT NULL,
                                             `password_hash`       VARCHAR(255) NOT NULL,
                                             `created_at`          DATETIME     NOT NULL,
                                             `created_by`          VARCHAR(29)  NOT NULL,
                                             `updated_at`          DATETIME     NOT NULL,
                                             `updated_by`          VARCHAR(29)  NOT NULL,
                                             CONSTRAINT `PK_password_histories` PRIMARY KEY (`password_history_id`),
                                             CONSTRAINT `FK_password_histories_user`
                                                 FOREIGN KEY (`user_id`) REFERENCES `auth`.`credentials` (`user_id`),
                                             CONSTRAINT `UQ_password_histories_generation` UNIQUE (`user_id`, `generation_no`)
);

-- リフレッシュトークン（書き込みが多いため外部キーなし。）
CREATE TABLE `auth`.`refresh_tokens` (
                                         `refresh_token_id` CHAR(29)    NOT NULL,
                                         `user_id`          CHAR(29)    NOT NULL,
                                         `token_hash`       CHAR(64)    NOT NULL,
                                         `family_id`        CHAR(29)    NOT NULL,
                                         `expires_at`       DATETIME    NOT NULL,
                                         `revoked_at`       DATETIME    NULL,
                                         `created_at`       DATETIME    NOT NULL,
                                         `created_by`       VARCHAR(29) NOT NULL,
                                         `updated_at`       DATETIME    NOT NULL,
                                         `updated_by`       VARCHAR(29) NOT NULL,
                                         CONSTRAINT `PK_refresh_tokens` PRIMARY KEY (`refresh_token_id`),
                                         CONSTRAINT `UQ_refresh_tokens_hash` UNIQUE (`token_hash`),
                                         INDEX `IDX_refresh_tokens_user` (`user_id`),
                                         INDEX `IDX_refresh_tokens_family` (`family_id`)
);