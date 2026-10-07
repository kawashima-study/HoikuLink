-- 連絡帳モジュールのテーブル（テーブル定義 第3版）
-- モジュールの外（nursery の園・園児・保育士・保護者）を指すIDには外部キーを付けない
-- 画像は大量データのため、連絡帳への外部キーも付けない（同じ集約としてまとめて保存・削除する。ADR-82）

-- 保育士連絡帳（項目の並びは画面「保育士作成」の順）
CREATE TABLE `diary`.`teacher_diaries` (
                                           `teacher_diary_id`        CHAR(29)      NOT NULL,
                                           `nursery_id`              CHAR(29)      NOT NULL,
                                           `child_id`                CHAR(29)      NOT NULL,
                                           `author_teacher_id`       CHAR(29)      NOT NULL,
                                           `author_name`             VARCHAR(100)  NOT NULL,
                                           `child_name`              VARCHAR(100)  NOT NULL,
                                           `class_name`              VARCHAR(50)   NOT NULL,
                                           `target_date`             DATE          NOT NULL,
                                           `condition_am`            VARCHAR(200)  NULL,
                                           `condition_pm`            VARCHAR(200)  NULL,
                                           `temperature`             DECIMAL(3,1)  NULL,
                                           `temperature_measured_at` TIME          NULL,
                                           `mood_am`                 VARCHAR(20)   NULL,
                                           `mood_pm`                 VARCHAR(20)   NULL,
                                           `bowel_count_am`          TINYINT       NULL,
                                           `bowel_status_am`         VARCHAR(20)   NULL,
                                           `bowel_count_pm`          TINYINT       NULL,
                                           `bowel_status_pm`         VARCHAR(20)   NULL,
                                           `lunch`                   VARCHAR(200)  NULL,
                                           `snack`                   VARCHAR(200)  NULL,
                                           `nap_start_at`            TIME          NULL,
                                           `nap_end_at`              TIME          NULL,
                                           `teacher_comment`         VARCHAR(1000) NULL,
                                           `version`                 INT           NOT NULL DEFAULT 1,
                                           `created_at`              DATETIME      NOT NULL,
                                           `created_by`              VARCHAR(29)   NOT NULL,
                                           `updated_at`              DATETIME      NOT NULL,
                                           `updated_by`              VARCHAR(29)   NOT NULL,
                                           `deleted_at`              DATETIME      NULL,
                                           `active_flag`             TINYINT
                                               GENERATED ALWAYS AS (CASE WHEN `deleted_at` IS NULL THEN 1 ELSE NULL END) STORED,
                                           CONSTRAINT `PK_teacher_diaries` PRIMARY KEY (`teacher_diary_id`),
                                           CONSTRAINT `UQ_teacher_diaries_child_date` UNIQUE (`child_id`, `target_date`, `active_flag`),
                                           INDEX `IDX_teacher_diaries_search` (`nursery_id`, `child_id`, `target_date`)
);

-- 保育士連絡帳画像（1連絡帳5枚まで。枚数は集約が守る）
CREATE TABLE `diary`.`teacher_diary_images` (
                                                `teacher_diary_image_id` CHAR(29)     NOT NULL,
                                                `teacher_diary_id`       CHAR(29)     NOT NULL,
                                                `s3_object_key`          VARCHAR(255) NOT NULL,
                                                `content_type`           VARCHAR(20)  NOT NULL,
                                                `file_size_bytes`        INT          NOT NULL,
                                                `display_order`          TINYINT      NOT NULL DEFAULT 1,
                                                `created_at`             DATETIME     NOT NULL,
                                                `created_by`             VARCHAR(29)  NOT NULL,
                                                `updated_at`             DATETIME     NOT NULL,
                                                `updated_by`             VARCHAR(29)  NOT NULL,
                                                `deleted_at`             DATETIME     NULL,
                                                CONSTRAINT `PK_teacher_diary_images` PRIMARY KEY (`teacher_diary_image_id`),
                                                CONSTRAINT `UQ_teacher_diary_images_key` UNIQUE (`s3_object_key`),
                                                INDEX `IDX_teacher_diary_images_diary` (`teacher_diary_id`, `display_order`)
);

-- 保護者連絡帳（項目の並びは画面「保護者作成」の順）
CREATE TABLE `diary`.`parent_diaries` (
                                          `parent_diary_id`         CHAR(29)      NOT NULL,
                                          `nursery_id`              CHAR(29)      NOT NULL,
                                          `child_id`                CHAR(29)      NOT NULL,
                                          `author_parent_id`        CHAR(29)      NOT NULL,
                                          `author_name`             VARCHAR(100)  NOT NULL,
                                          `child_name`              VARCHAR(100)  NOT NULL,
                                          `class_name`              VARCHAR(50)   NOT NULL,
                                          `target_date`             DATE          NOT NULL,
                                          `condition_night`         VARCHAR(200)  NULL,
                                          `condition_morning`       VARCHAR(200)  NULL,
                                          `temperature`             DECIMAL(3,1)  NULL,
                                          `temperature_measured_at` TIME          NULL,
                                          `mood_night`              VARCHAR(20)   NULL,
                                          `mood_morning`            VARCHAR(20)   NULL,
                                          `bowel_count_night`       TINYINT       NULL,
                                          `bowel_status_night`      VARCHAR(20)   NULL,
                                          `bowel_count_morning`     TINYINT       NULL,
                                          `bowel_status_morning`    VARCHAR(20)   NULL,
                                          `dinner`                  VARCHAR(200)  NULL,
                                          `breakfast`               VARCHAR(200)  NULL,
                                          `sleep_start_at`          TIME          NULL,
                                          `sleep_end_at`            TIME          NULL,
                                          `parent_comment`          VARCHAR(1000) NULL,
                                          `version`                 INT           NOT NULL DEFAULT 1,
                                          `created_at`              DATETIME      NOT NULL,
                                          `created_by`              VARCHAR(29)   NOT NULL,
                                          `updated_at`              DATETIME      NOT NULL,
                                          `updated_by`              VARCHAR(29)   NOT NULL,
                                          `deleted_at`              DATETIME      NULL,
                                          `active_flag`             TINYINT
                                              GENERATED ALWAYS AS (CASE WHEN `deleted_at` IS NULL THEN 1 ELSE NULL END) STORED,
                                          CONSTRAINT `PK_parent_diaries` PRIMARY KEY (`parent_diary_id`),
                                          CONSTRAINT `UQ_parent_diaries_child_date` UNIQUE (`child_id`, `target_date`, `active_flag`),
                                          INDEX `IDX_parent_diaries_search` (`nursery_id`, `child_id`, `target_date`)
);

-- 保護者連絡帳画像（1連絡帳5枚まで）
CREATE TABLE `diary`.`parent_diary_images` (
                                               `parent_diary_image_id` CHAR(29)     NOT NULL,
                                               `parent_diary_id`       CHAR(29)     NOT NULL,
                                               `s3_object_key`         VARCHAR(255) NOT NULL,
                                               `content_type`          VARCHAR(20)  NOT NULL,
                                               `file_size_bytes`       INT          NOT NULL,
                                               `display_order`         TINYINT      NOT NULL DEFAULT 1,
                                               `created_at`            DATETIME     NOT NULL,
                                               `created_by`            VARCHAR(29)  NOT NULL,
                                               `updated_at`            DATETIME     NOT NULL,
                                               `updated_by`            VARCHAR(29)  NOT NULL,
                                               `deleted_at`            DATETIME     NULL,
                                               CONSTRAINT `PK_parent_diary_images` PRIMARY KEY (`parent_diary_image_id`),
                                               CONSTRAINT `UQ_parent_diary_images_key` UNIQUE (`s3_object_key`),
                                               INDEX `IDX_parent_diary_images_diary` (`parent_diary_id`, `display_order`)
);

-- 連携事項（公開版はテストデータでトップに表示するだけ。ADR-93）
-- 作成日時だけミリ秒まで持ち、作成順に厳密に並べる（ADR-81の例外）
CREATE TABLE `diary`.`handover_notes` (
                                          `handover_note_id`  CHAR(29)     NOT NULL,
                                          `nursery_id`        CHAR(29)     NOT NULL,
                                          `child_id`          CHAR(29)     NOT NULL,
                                          `author_teacher_id` CHAR(29)     NOT NULL,
                                          `author_name`       VARCHAR(100) NOT NULL,
                                          `child_name`        VARCHAR(100) NOT NULL,
                                          `content`           VARCHAR(500) NOT NULL,
                                          `created_at`        DATETIME(3)  NOT NULL,
                                          `created_by`        VARCHAR(29)  NOT NULL,
                                          `updated_at`        DATETIME     NOT NULL,
                                          `updated_by`        VARCHAR(29)  NOT NULL,
                                          `deleted_at`        DATETIME     NULL,
                                          CONSTRAINT `PK_handover_notes` PRIMARY KEY (`handover_note_id`),
                                          INDEX `IDX_handover_notes_top` (`nursery_id`, `child_id`, `created_at`)
);