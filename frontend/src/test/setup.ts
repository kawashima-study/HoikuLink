import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'
// 画面のテストで使う確認の書き方（toBeInTheDocumentなど）を使えるようにする
import '@testing-library/jest-dom/vitest'

// テストごとに表示した画面を片付ける（Vitestのglobalsを使わない設定では自動で片付かないため）
afterEach(() => {
  cleanup()
})
