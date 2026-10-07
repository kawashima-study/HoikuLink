-- 手元の開発・テスト専用のユーザー（本番では使わない）
-- MySQLの初回起動時に、/docker-entrypoint-initdb.d から自動で実行される

-- マイグレーション用：スキーマとテーブルを作る・変える
CREATE USER IF NOT EXISTS 'hoikulink_migrator'@'%' IDENTIFIED BY 'localmigratorpassword';
GRANT ALL PRIVILEGES ON `hoikulink`.* TO 'hoikulink_migrator'@'%';
GRANT ALL PRIVILEGES ON `auth`.* TO 'hoikulink_migrator'@'%';
GRANT ALL PRIVILEGES ON `nursery`.* TO 'hoikulink_migrator'@'%';
GRANT ALL PRIVILEGES ON `diary`.* TO 'hoikulink_migrator'@'%';

-- アプリ用：データの読み書きだけ
GRANT SELECT, INSERT, UPDATE, DELETE ON `auth`.* TO 'hoikulink_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `nursery`.* TO 'hoikulink_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `diary`.* TO 'hoikulink_app'@'%';