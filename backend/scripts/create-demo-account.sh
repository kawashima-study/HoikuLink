#!/bin/bash
# 手元の開発用のデモのアカウントを作る（本番では使わない）
# 使い方：backendフォルダで ./gradlew bootRun を起動したまま 別のターミナルで bash scripts/create-demo-account.sh
# メールアドレス demo@example.com パスワード hoikulink-demo-2026
set -e
cd "$(dirname "$0")/.."

EMAIL='demo@example.com'
PASSWORD='hoikulink-demo-2026'
USER_ID='USR01JCDEMQ0000000000000000A1'

# 本番と同じ設定（メモリ12MiB・反復3回・並列度1）のArgon2idでパスワードのハッシュ値を作る
# Macに道具を入れずに済むようDockerのPythonで計算する
HASH=$(docker run --rm python:3.12-slim sh -c \
  "pip install -q --disable-pip-version-check argon2-cffi >/dev/null 2>&1 && python -c \"from argon2 import PasswordHasher; print(PasswordHasher(time_cost=3, memory_cost=12288, parallelism=1, hash_len=32, salt_len=16).hash('$PASSWORD'))\"")

# アカウントを入れる（すでにある場合はパスワードとロックの状態を元に戻す）
docker compose exec -T mysql mysql -uroot -plocalrootpassword -e "
INSERT INTO auth.credentials
  (user_id, email, password_hash, created_at, created_by, updated_at, updated_by)
VALUES
  ('$USER_ID', '$EMAIL', '$HASH',
   CONVERT_TZ(UTC_TIMESTAMP(), '+00:00', '+09:00'), '$USER_ID',
   CONVERT_TZ(UTC_TIMESTAMP(), '+00:00', '+09:00'), '$USER_ID')
AS new_row
ON DUPLICATE KEY UPDATE
  password_hash = new_row.password_hash,
  failed_login_count = 0,
  locked_at = NULL;"

echo "デモのアカウントを作りました（${EMAIL}）"
