import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const apiProxyTarget = process.env.VITE_API_PROXY_TARGET ?? 'http://localhost:8081'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined
          if (id.includes('@ant-design/icons')) return 'vendor-icons'
          if (id.includes('@ant-design/pro-components')) return 'vendor-pro-components'
          if (id.includes('@tanstack/react-query')) return 'vendor-react-query'
          if (id.includes('react') || id.includes('react-router-dom')) return 'vendor-react'
          return undefined
        },
      },
    },
  },
  define: {
    process: { env: { NODE_ENV: process.env.NODE_ENV ?? 'development' } },
    'process.env.NODE_ENV': JSON.stringify(process.env.NODE_ENV ?? 'development'),
  },
  server: {
    port: 5173,
    // 加上这一行，允许 ngrok 访问
    allowedHosts: true,
    proxy: {
      '/api': { target: apiProxyTarget, changeOrigin: true },
      '/files': { target: apiProxyTarget, changeOrigin: true },
    },
  },
})
