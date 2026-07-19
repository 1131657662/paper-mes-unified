import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const apiProxyTarget = process.env.VITE_API_PROXY_TARGET ?? 'http://localhost:8081'
const apiProxy = {
  '/api': { target: apiProxyTarget, changeOrigin: true },
  '/files': { target: apiProxyTarget, changeOrigin: true },
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined
          if (id.includes('@ant-design/pro-components')) return 'vendor-pro-components'
          if (id.includes('@tanstack/react-query')) return 'vendor-react-query'
          if (isReactRuntime(id)) return 'vendor-react'
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
    proxy: apiProxy,
  },
  preview: {
    port: 4173,
    allowedHosts: true,
    proxy: apiProxy,
  },
})

function isReactRuntime(id: string) {
  return [
    '/node_modules/react/',
    '/node_modules/react-dom/',
    '/node_modules/react-router/',
    '/node_modules/react-router-dom/',
    '/node_modules/scheduler/',
  ].some((packagePath) => id.includes(packagePath))
}
