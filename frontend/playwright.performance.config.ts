import { defineConfig, devices } from '@playwright/test'

const baseURL = process.env.PAPER_MES_PERF_BASE_URL?.trim() || 'http://127.0.0.1:4177'

export default defineConfig({
  testDir: './e2e',
  testMatch: '**/*-performance.ts',
  outputDir: 'test-results/performance',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: 'list',
  projects: [
    {
      name: 'desktop-1366',
      use: { ...devices['Desktop Chrome'], viewport: { width: 1366, height: 768 } },
    },
    {
      name: 'desktop-1440',
      use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } },
    },
    {
      name: 'desktop-1920',
      use: { ...devices['Desktop Chrome'], viewport: { width: 1920, height: 1080 } },
    },
  ],
  use: {
    baseURL,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
})
