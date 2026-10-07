-- 園運営モジュールのテーブル（テーブル定義 第3版）
-- 外部キーは同じモジュールの中の、データが少ないテーブルだけに付ける（ADR-82）
-- モジュールの外（auth.credentials）を指すIDには付けない

-- 園
CREATE TABLE `nursery`.`nurseries` (
                                       `nursery_id`            CHAR(29)     NOT NULL,
                                       `nursery_name`          VARCHAR(100) NOT NULL,
                                       `status`                VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
                                       `contract_end_date`     DATE         NULL,
                                       `diary_retention_years` TINYINT      NOT NULL DEFAULT 5,
                                       `postal_code`           VARCHAR(8)   NULL,
                                       `prefecture`            VARCHAR(20)  NULL,
                                       `city`                  VARCHAR(100) NULL,
                                       `address`               VARCHAR(255) NULL,
                                       `phone_number`          VARCHAR(20)  NULL,
                                       `created_at`            DATETIME     NOT NULL,
                                       `created_by`            VARCHAR(29)  NOT NULL,
                                       `updated_at`            DATETIME     NOT NULL,
                                       `updated_by`            VARCHAR(29)  NOT NULL,
                                       `deleted_at`            DATETIME     NULL,
                                       CONSTRAINT `PK_nurseries` PRIMARY KEY (`nursery_id`)
);

-- 役職（園ごと）
CREATE TABLE `nursery`.`nursery_roles` (
                                           `role_id`     CHAR(29)    NOT NULL,
                                           `nursery_id`  CHAR(29)    NOT NULL,
                                           `role_name`   VARCHAR(50) NOT NULL,
                                           `role_rank`   TINYINT     NOT NULL,
                                           `created_at`  DATETIME    NOT NULL,
                                           `created_by`  VARCHAR(29) NOT NULL,
                                           `updated_at`  DATETIME    NOT NULL,
                                           `updated_by`  VARCHAR(29) NOT NULL,
                                           `deleted_at`  DATETIME    NULL,
                                           `active_flag` TINYINT
                                               GENERATED ALWAYS AS (CASE WHEN `deleted_at` IS NULL THEN 1 ELSE NULL END) STORED,
                                           CONSTRAINT `PK_nursery_roles` PRIMARY KEY (`role_id`),
                                           CONSTRAINT `FK_nursery_roles_nursery`
                                               FOREIGN KEY (`nursery_id`) REFERENCES `nursery`.`nurseries` (`nursery_id`),
                                           CONSTRAINT `UQ_nursery_roles_rank` UNIQUE (`nursery_id`, `role_rank`, `active_flag`)
);

-- 権限マスタ（全園共通。Flywayで投入し、画面からは変更しないので監査項目なし）
CREATE TABLE `nursery`.`mst_permissions` (
                                             `permission_code` VARCHAR(50)  NOT NULL,
                                             `permission_name` VARCHAR(100) NOT NULL,
                                             `display_order`   INT          NOT NULL,
                                             CONSTRAINT `PK_mst_permissions` PRIMARY KEY (`permission_code`)
);

-- 役職権限（付け外しは行の追加・削除）
CREATE TABLE `nursery`.`role_permissions` (
                                              `role_id`         CHAR(29)    NOT NULL,
                                              `permission_code` VARCHAR(50) NOT NULL,
                                              `created_at`      DATETIME    NOT NULL,
                                              `created_by`      VARCHAR(29) NOT NULL,
                                              `updated_at`      DATETIME    NOT NULL,
                                              `updated_by`      VARCHAR(29) NOT NULL,
                                              CONSTRAINT `PK_role_permissions` PRIMARY KEY (`role_id`, `permission_code`),
                                              CONSTRAINT `FK_role_permissions_role`
                                                  FOREIGN KEY (`role_id`) REFERENCES `nursery`.`nursery_roles` (`role_id`),
                                              CONSTRAINT `FK_role_permissions_permission`
                                                  FOREIGN KEY (`permission_code`) REFERENCES `nursery`.`mst_permissions` (`permission_code`),
                                              INDEX `IDX_role_permissions_permission` (`permission_code`)
);

-- クラス
CREATE TABLE `nursery`.`classes` (
                                     `class_id`      CHAR(29)    NOT NULL,
                                     `nursery_id`    CHAR(29)    NOT NULL,
                                     `class_name`    VARCHAR(50) NOT NULL,
                                     `display_order` INT         NOT NULL DEFAULT 1,
                                     `created_at`    DATETIME    NOT NULL,
                                     `created_by`    VARCHAR(29) NOT NULL,
                                     `updated_at`    DATETIME    NOT NULL,
                                     `updated_by`    VARCHAR(29) NOT NULL,
                                     `deleted_at`    DATETIME    NULL,
                                     CONSTRAINT `PK_classes` PRIMARY KEY (`class_id`),
                                     CONSTRAINT `FK_classes_nursery`
                                         FOREIGN KEY (`nursery_id`) REFERENCES `nursery`.`nurseries` (`nursery_id`),
                                     INDEX `IDX_classes_nursery` (`nursery_id`, `display_order`)
);

-- 保育士（園ごとの所属。兼務は同じユーザーIDで2行）
CREATE TABLE `nursery`.`teachers` (
                                      `teacher_id`  CHAR(29)     NOT NULL,
                                      `user_id`     CHAR(29)     NOT NULL,
                                      `nursery_id`  CHAR(29)     NOT NULL,
                                      `role_id`     CHAR(29)     NOT NULL,
                                      `last_name`   VARCHAR(50)  NOT NULL,
                                      `first_name`  VARCHAR(50)  NOT NULL,
                                      `email`       VARCHAR(255) NOT NULL,
                                      `started_on`  DATE         NOT NULL,
                                      `ended_on`    DATE         NULL,
                                      `created_at`  DATETIME     NOT NULL,
                                      `created_by`  VARCHAR(29)  NOT NULL,
                                      `updated_at`  DATETIME     NOT NULL,
                                      `updated_by`  VARCHAR(29)  NOT NULL,
                                      `deleted_at`  DATETIME     NULL,
                                      `active_flag` TINYINT
                                          GENERATED ALWAYS AS (CASE WHEN `deleted_at` IS NULL THEN 1 ELSE NULL END) STORED,
                                      CONSTRAINT `PK_teachers` PRIMARY KEY (`teacher_id`),
                                      CONSTRAINT `FK_teachers_nursery`
                                          FOREIGN KEY (`nursery_id`) REFERENCES `nursery`.`nurseries` (`nursery_id`),
                                      CONSTRAINT `FK_teachers_role`
                                          FOREIGN KEY (`role_id`) REFERENCES `nursery`.`nursery_roles` (`role_id`),
                                      CONSTRAINT `UQ_teachers_user_nursery` UNIQUE (`user_id`, `nursery_id`, `active_flag`)
);

-- 担当（付け外しは行の追加・削除）
CREATE TABLE `nursery`.`class_assignments` (
                                               `teacher_id` CHAR(29)    NOT NULL,
                                               `class_id`   CHAR(29)    NOT NULL,
                                               `created_at` DATETIME    NOT NULL,
                                               `created_by` VARCHAR(29) NOT NULL,
                                               `updated_at` DATETIME    NOT NULL,
                                               `updated_by` VARCHAR(29) NOT NULL,
                                               CONSTRAINT `PK_class_assignments` PRIMARY KEY (`teacher_id`, `class_id`),
                                               CONSTRAINT `FK_class_assignments_teacher`
                                                   FOREIGN KEY (`teacher_id`) REFERENCES `nursery`.`teachers` (`teacher_id`),
                                               CONSTRAINT `FK_class_assignments_class`
                                                   FOREIGN KEY (`class_id`) REFERENCES `nursery`.`classes` (`class_id`),
                                               INDEX `IDX_class_assignments_class` (`class_id`)
);

-- 保護者（公開版ではログインしない）
CREATE TABLE `nursery`.`parents` (
                                     `parent_id`  CHAR(29)     NOT NULL,
                                     `user_id`    CHAR(29)     NULL,
                                     `last_name`  VARCHAR(50)  NOT NULL,
                                     `first_name` VARCHAR(50)  NOT NULL,
                                     `email`      VARCHAR(255) NULL,
                                     `created_at` DATETIME     NOT NULL,
                                     `created_by` VARCHAR(29)  NOT NULL,
                                     `updated_at` DATETIME     NOT NULL,
                                     `updated_by` VARCHAR(29)  NOT NULL,
                                     `deleted_at` DATETIME     NULL,
                                     CONSTRAINT `PK_parents` PRIMARY KEY (`parent_id`)
);

-- 園児（メイン保護者は必須。ADR-86）
CREATE TABLE `nursery`.`children` (
                                      `child_id`       CHAR(29)    NOT NULL,
                                      `nursery_id`     CHAR(29)    NOT NULL,
                                      `class_id`       CHAR(29)    NOT NULL,
                                      `last_name`      VARCHAR(50) NOT NULL,
                                      `first_name`     VARCHAR(50) NOT NULL,
                                      `birthday`       DATE        NOT NULL,
                                      `gender`         VARCHAR(20) NOT NULL,
                                      `enrolled_on`    DATE        NOT NULL,
                                      `left_on`        DATE        NULL,
                                      `main_parent_id` CHAR(29)    NOT NULL,
                                      `created_at`     DATETIME    NOT NULL,
                                      `created_by`     VARCHAR(29) NOT NULL,
                                      `updated_at`     DATETIME    NOT NULL,
                                      `updated_by`     VARCHAR(29) NOT NULL,
                                      `deleted_at`     DATETIME    NULL,
                                      CONSTRAINT `PK_children` PRIMARY KEY (`child_id`),
                                      CONSTRAINT `FK_children_nursery`
                                          FOREIGN KEY (`nursery_id`) REFERENCES `nursery`.`nurseries` (`nursery_id`),
                                      CONSTRAINT `FK_children_class`
                                          FOREIGN KEY (`class_id`) REFERENCES `nursery`.`classes` (`class_id`),
                                      CONSTRAINT `FK_children_main_parent`
                                          FOREIGN KEY (`main_parent_id`) REFERENCES `nursery`.`parents` (`parent_id`),
                                      INDEX `IDX_children_nursery_class` (`nursery_id`, `class_id`),
                                      INDEX `IDX_children_main_parent` (`main_parent_id`)
);

-- 保護者園児紐づけ
CREATE TABLE `nursery`.`parent_children` (
                                             `parent_child_id` CHAR(29)    NOT NULL,
                                             `parent_id`       CHAR(29)    NOT NULL,
                                             `child_id`        CHAR(29)    NOT NULL,
                                             `relationship`    VARCHAR(20) NOT NULL,
                                             `started_on`      DATE        NOT NULL,
                                             `ended_on`        DATE        NULL,
                                             `created_at`      DATETIME    NOT NULL,
                                             `created_by`      VARCHAR(29) NOT NULL,
                                             `updated_at`      DATETIME    NOT NULL,
                                             `updated_by`      VARCHAR(29) NOT NULL,
                                             `deleted_at`      DATETIME    NULL,
                                             `active_flag`     TINYINT
                                                 GENERATED ALWAYS AS (CASE WHEN `deleted_at` IS NULL THEN 1 ELSE NULL END) STORED,
                                             CONSTRAINT `PK_parent_children` PRIMARY KEY (`parent_child_id`),
                                             CONSTRAINT `FK_parent_children_parent`
                                                 FOREIGN KEY (`parent_id`) REFERENCES `nursery`.`parents` (`parent_id`),
                                             CONSTRAINT `FK_parent_children_child`
                                                 FOREIGN KEY (`child_id`) REFERENCES `nursery`.`children` (`child_id`),
                                             CONSTRAINT `UQ_parent_children` UNIQUE (`parent_id`, `child_id`, `active_flag`),
                                             INDEX `IDX_parent_children_child` (`child_id`)
);