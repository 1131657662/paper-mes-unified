import { defineConfig, devices } from '@playwright/test'

const configuredBaseURL = process.env.PAPER_MES_E2E_BASE_URL?.trim()
const baseURL = configuredBaseURL || 'http://127.0.0.1:5176'
const managesLocalServer = !configuredBaseURL

export default defineConfig({
  testDir: './e2e',
  testMatch: '**/*.e2e.ts',
  outputDir: 'test-results',
  fullyParallel: false,
  retries: 0,
  reporter: 'list',
  projects: [
    {
      name: 'desktop-1366',
      use: {
        ...devices['Desktop Chrome'],
        viewport: { width: 1366, height: 768 },
      },
    },
    {
      name: 'desktop-1440',
      use: {
        ...devices['Desktop Chrome'],
        viewport: { width: 1440, height: 900 },
      },
    },
    {
      name: 'desktop-1920',
      use: {
        ...devices['Desktop Chrome'],
        viewport: { width: 1920, height: 1080 },
      },
    },
  ],
  webServer: managesLocalServer
    ? {
        command: 'npm run dev -- --host 127.0.0.1 --port 5176 --strictPort',
        reuseExistingServer: true,
        timeout: 60_000,
        url: baseURL,
      }
    : undefined,
  use: {
    baseURL,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
})
