import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    // 开发期代理：前端请求 /api/** → 后端 http://localhost:9321/**
    // 好处：① 免 CORS ② 前后端同源，生产只需改 VITE_API_BASE 指向真实域名
    proxy: {
      '/api': {
        target: 'http://localhost:9321',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  },
  build: {
    // 大依赖单独分包：echarts/element-plus 变更频率低，
    // 拆出 vendor chunk 后业务代码更新不影响浏览器缓存命中
    rollupOptions: {
      output: {
        manualChunks: {
          echarts: ['echarts'],
          'element-plus': ['element-plus', '@element-plus/icons-vue']
        }
      }
    }
  }
})
