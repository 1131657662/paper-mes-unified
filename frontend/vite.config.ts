import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { visualizer } from 'rollup-plugin-visualizer'
import { fileURLToPath } from 'node:url'

const apiProxyTarget = process.env.VITE_API_PROXY_TARGET ?? 'http://localhost:8081'
const apiProxy = {
  '/api': { target: apiProxyTarget, changeOrigin: true },
  '/files': { target: apiProxyTarget, changeOrigin: true },
}

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const nodeEnv = process.env.NODE_ENV ?? (mode === 'development' ? 'development' : 'production')
  const shouldAnalyze = mode === 'analyze' || process.env.ANALYZE === 'true'
  const shouldProfile = process.env.VITE_PERF_PROFILER_ENABLED === 'true'
  const allowedHosts = parseAllowedHosts(process.env.VITE_ALLOWED_HOSTS)

  return {
    plugins: [
      react(),
      ...(shouldAnalyze ? [visualizer({
        filename: 'artifacts/bundle.html',
        gzipSize: true,
        brotliSize: true,
        open: false,
      })] : []),
    ],
    build: {
      target: ['chrome109', 'edge109'],
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (!id.includes('node_modules')) return undefined
            if (isAntdIcon(id)) return 'vendor-icons'
            if (id.includes('@ant-design/pro-components')) return 'vendor-pro-components'
            if (id.includes('@tanstack/react-query')) return 'vendor-react-query'
            if (isReactRuntime(id)) return 'vendor-react'
            return undefined
          },
        },
      },
    },
    define: {
      process: { env: { NODE_ENV: nodeEnv } },
      'process.env.NODE_ENV': JSON.stringify(nodeEnv),
    },
    resolve: {
      alias: shouldProfile
        ? [{ find: 'react-dom/client', replacement: fileURLToPath(new URL('./node_modules/react-dom/profiling.js', import.meta.url)) }]
        : [],
    },
    server: {
      port: 5173,
      allowedHosts,
      proxy: apiProxy,
    },
    preview: {
      port: 4173,
      allowedHosts,
      proxy: apiProxy,
    },
  }
})

function parseAllowedHosts(value: string | undefined): string[] {
  const hosts = value?.split(',').map((host) => host.trim()).filter(Boolean)
  return hosts?.length ? hosts : ['localhost', '127.0.0.1']
}

function isReactRuntime(id: string) {
  return [
    '/node_modules/react/',
    '/node_modules/react-dom/',
    '/node_modules/react-router/',
    '/node_modules/react-router-dom/',
    '/node_modules/scheduler/',
  ].some((packagePath) => id.includes(packagePath))
}

function isAntdIcon(id: string) {
  return [
    '/node_modules/@ant-design/icons/',
    '/node_modules/@ant-design/icons-svg/',
  ].some((packagePath) => id.includes(packagePath))
}
