# NurseryDiary

保育施設向けSaaSを想定した、保育士用の連絡帳Webアプリです。
要件定義・設計・実装・AWSでの公開までを一人で一気通貫に行うポートフォリオです。

## ステータス

開発中（2026年11月12日公開予定）

## 技術スタック（予定）

### バックエンド

| 分類 | 技術 |
|---|---|
| 言語 | Java 21 |
| フレームワーク | Spring Boot 4.1 / Spring Security |
| アーキテクチャ | モジュラーモノリス（Spring Modulith）/ クリーンアーキテクチャ / DDD |
| DBアクセス | jOOQ |
| DBマイグレーション | Flyway |
| ビルド | Gradle（Kotlin DSL） |
| テスト | JUnit / Testcontainers / ArchUnit |

### フロントエンド

| 分類 | 技術 |
|---|---|
| 言語 | TypeScript |
| フレームワーク | React |
| ビルド | Vite |

### データベース・キャッシュ

| 分類 | 技術 |
|---|---|
| データベース | Aurora MySQL（ローカル：MySQL） |
| キャッシュ | ElastiCache（ローカル：Redis） |
| ファイル保存 | Amazon S3（ローカル：S3互換ツール） |

### インフラ

| 分類 | 技術 |
|---|---|
| クラウド | AWS（ECS Fargate / ALB / CloudFront / S3 / Aurora / ElastiCache / WAF） |
| IaC | AWS CDK（Java） |
| コンテナ | Docker / Docker Compose |
| 監視 | Amazon CloudWatch |

### 開発プロセス

| 分類 | 内容 |
|---|---|
| 開発手法 | アジャイル＋XP（domain・application層はTDD） |
| CI/CD | GitHub Actions |
| ブランチ運用 | GitHub Flow |