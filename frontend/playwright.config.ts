import { defineConfig, devices } from '@playwright/test'

const baseURL = process.env.PAPER_MES_E2E_BASE_URL ?? 'http://127.0.0.1:5176'
const managesLocalServer = !process.env.PAPER_MES_E2E_BASE_URL

export default defineConfig({
  testDir: './e2e',
  testMatch: '**/*.e2e.ts',
  outputDir: 'test-results',
  fullyParallel: false,
  retries: 0,
  reporter: 'list',
  webServer: managesLocalServer ? {
    command: 'npm run dev -- --host 127.0.0.1 --port 5176 --strictPort',
    reuseExistingServer: true,
    timeout: 60_000,
    url: baseURL,
  } : undefined,
  use: {
    ...devices['Desktop Chrome'],
    baseURL,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
})
