# HoikuLink 画面（frontend）

保育士向け連絡帳システムHoikuLinkの画面。React＋TypeScript＋Viteで作成。

## 使用技術

| 分類 | 技術 |
|---|---|
| 画面 | React、TypeScript、React Router |
| ビルド・開発用サーバー | Vite |
| テスト | Vitest、Testing Library |
| Lint | ESLint |

## 手元での起動

APIを `localhost:8080` で起動した状態で、次を実行する。開発用サーバーが `/api` をAPIに中継する。

```bash
npm install
npm run dev
```

ブラウザで http://localhost:5173 を開く。

## コマンド

| コマンド | 内容 |
|---|---|
| `npm run dev` | 開発用サーバーを起動する |
| `npm test` | テストを実行する |
| `npm run lint` | Lintを実行する |
| `npm run build` | 型の確認と本番用のビルドを行う |

## 設計の要点

- Access Tokenはメモリ（変数）にだけ持ち、ブラウザのストレージには保存しない
- ページを開いたときは、HttpOnlyのCookieのRefresh Tokenで再発行してログインを取り戻す
- 同時に呼ばれた再発行は1回にまとめる（同じRefresh Tokenで2回再発行すると、使い回しと判定されるため）
