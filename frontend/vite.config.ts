/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// 画面の開発用サーバーとテストの設定
export default defineConfig({
  plugins: [react()],
  server: {
    // APIへのリクエストを手元のAPI（localhost:8080）へ中継する
    // 画面とAPIを同じ送信元として扱いリフレッシュトークンのCookieを送れるようにするため
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom', // ブラウザの代わりの環境で画面の部品を動かす
    setupFiles: './src/test/setup.ts',
  },
})
