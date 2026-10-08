-- 権限マスタの初期データ
-- 権限の種類はコードの PermissionCode で定義し、同じ値をここに入れる
INSERT INTO `nursery`.`mst_permissions` (`permission_code`, `permission_name`, `display_order`) VALUES
                                                                                                    ('CHILD_VIEW_ASSIGNED',           '担当園児の閲覧',                         1),
                                                                                                    ('CHILD_VIEW_ALL',                '全園児の閲覧',                           2),
                                                                                                    ('TEACHER_DIARY_CREATE_ASSIGNED', '担当園児の保育士連絡帳を作成',           3),
                                                                                                    ('TEACHER_DIARY_CREATE_ALL',      '全園児の保育士連絡帳を作成',             4),
                                                                                                    ('TEACHER_DIARY_VIEW_ASSIGNED',   '担当園児の保育士連絡帳を閲覧',           5),
                                                                                                    ('TEACHER_DIARY_VIEW_ALL',        '全園児の保育士連絡帳を閲覧',             6),
                                                                                                    ('TEACHER_DIARY_EDIT_OWN',        '自分の保育士連絡帳を編集',               7),
                                                                                                    ('TEACHER_DIARY_EDIT_ALL',        '全ての保育士の保育士連絡帳を編集',       8),
                                                                                                    ('TEACHER_DIARY_DELETE_OWN',      '自分の保育士連絡帳を削除',               9),
                                                                                                    ('TEACHER_DIARY_DELETE_ALL',      '全ての保育士の保育士連絡帳を削除',      10),
                                                                                                    ('PARENT_DIARY_VIEW_ASSIGNED',    '担当園児の保護者連絡帳を閲覧',          11),
                                                                                                    ('PARENT_DIARY_VIEW_ALL',         '全園児の保護者連絡帳を閲覧',            12),
                                                                                                    ('PARENT_DIARY_IMAGE_DELETE',     '保護者連絡帳の画像を削除',              13),
                                                                                                    ('PARENT_DIARY_DELETE',           '保護者連絡帳を削除',                    14),
                                                                                                    ('ACCOUNT_UNLOCK',                'ロック中のアカウントを解除',            15);