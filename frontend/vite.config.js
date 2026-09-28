import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],

  server: {
    // 显式绑定 IPv4：Windows 下 localhost 可能优先解析为 ::1，
    // 只监听 IPv6 会导致 127.0.0.1 无法访问。
    host: '127.0.0.1',
    port: 5174,
    strictPort: true,

    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
})
