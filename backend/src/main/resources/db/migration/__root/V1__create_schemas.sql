-- モジュールごとのスキーマを作る（MySQLでは、スキーマ＝データベース）
CREATE DATABASE IF NOT EXISTS `auth`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_ja_0900_as_cs;

CREATE DATABASE IF NOT EXISTS `nursery`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_ja_0900_as_cs;

CREATE DATABASE IF NOT EXISTS `diary`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_ja_0900_as_cs;