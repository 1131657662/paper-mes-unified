import { expect, test, type Page, type Route } from '@playwright/test'

const PAGE_RECORD_COUNT = 50
const PROFILER_REVIEW_MS = 250

test('记录加工单表格 React Profiler commit p95', async ({ page }, testInfo) => {
  test.slow()
  await mockProcessOrderPage(page)
  await page.goto('/process-orders?size=50')

  const tableBody = page.locator('.process-order-table .ant-table-tbody-virtual-holder')
  await expect(tableBody).toBeVisible()
  await expect(page.getByText('PO-0001')).toBeVisible()
  await page.waitForTimeout(300)

  const metrics = await collectProfilerMetrics(page)
  await testInfo.attach('react-profiler-metrics.json', {
    body: Buffer.from(JSON.stringify(metrics, null, 2)),
    contentType: 'application/json',
  })
  console.info('[react-profiler-metrics]', JSON.stringify({
    project: testInfo.project.name,
    reviewRequired: metrics.p95ActualDurationMs > PROFILER_REVIEW_MS,
    reviewThresholdMs: PROFILER_REVIEW_MS,
    ...metrics,
  }))

  expect(metrics.commitCount).toBeGreaterThan(0)
  expect(metrics.renderedRows).toBeLessThan(PAGE_RECORD_COUNT)
})

async function collectProfilerMetrics(page: Page) {
  await page.evaluate(async () => {
    const holder = document.querySelector<HTMLElement>('.ant-table-tbody-virtual-holder')
    if (!holder) throw new Error('Virtual table scroll holder is missing')
    const maxScrollTop = holder.scrollHeight - holder.clientHeight
    await new Promise<void>((resolve) => {
      let frame = 0
      const sample = () => {
        holder.scrollTop = maxScrollTop * (++frame / 30)
        if (frame >= 30) resolve()
        else requestAnimationFrame(sample)
      }
      requestAnimationFrame(sample)
    })
  })

  return page.evaluate(() => {
    const raw = (window as Window & { __MES_PERF_PROFILER__?: unknown }).__MES_PERF_PROFILER__
    const commits = Array.isArray(raw)
      ? raw.filter((entry): entry is { actualDuration: number; id: string; phase: string } => (
        typeof entry === 'object'
        && entry !== null
        && 'actualDuration' in entry
        && typeof entry.actualDuration === 'number'
        && 'id' in entry
        && typeof entry.id === 'string'
        && 'phase' in entry
        && typeof entry.phase === 'string'
      ))
      : []
    const relevant = commits.filter((entry) => entry.id === 'process-order-list-table')
    const updates = relevant.filter((entry) => entry.phase !== 'mount')
    const durations = (updates.length ? updates : relevant)
      .map((entry) => entry.actualDuration)
      .sort((a, b) => a - b)
    const p95Index = Math.max(0, Math.ceil(durations.length * 0.95) - 1)
    return {
      commitCount: relevant.length,
      p95ActualDurationMs: durations[p95Index] ?? 0,
      renderedRows: document.querySelectorAll('.process-order-table .ant-table-tbody .ant-table-row').length,
      updateCommitCount: updates.length,
    }
  })
}

async function mockProcessOrderPage(page: Page): Promise<void> {
  await page.route('**/api/**', (route) => fulfillApi(route))
}

async function fulfillApi(route: Route): Promise<void> {
  const pathname = new URL(route.request().url()).pathname
  if (pathname === '/api/auth/me') return fulfill(route, { permissions: [], realName: 'E2E Admin', roleCode: 'admin', username: 'e2e-admin', uuid: 'e2e-admin' })
  if (pathname === '/api/process-orders') return fulfill(route, {
    current: 1,
    records: Array.from({ length: PAGE_RECORD_COUNT }, (_, index) => createProcessOrder(index)),
    size: PAGE_RECORD_COUNT,
    total: 59,
  })
  if (pathname === '/api/customers') return fulfill(route, { current: 1, records: [], size: 200, total: 0 })
  if (pathname === '/api/notifications') return fulfill(route, { items: [], unreadCount: 0 })
  if (pathname === '/api/export-tasks') return fulfill(route, { runningCount: 0, unacknowledgedCount: 0 })
  if (pathname === '/api/system/runtime/configs') return fulfill(route, [])
  if (pathname === '/api/export-tasks/events') return route.fulfill({ status: 200, contentType: 'text/event-stream', body: '' })
  return fulfill(route, {})
}

async function fulfill(route: Route, data: unknown): Promise<void> {
  await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 200, data, message: 'OK' }) })
}

function createProcessOrder(index: number) {
  const number = String(index + 1).padStart(4, '0')
  return { customerName: '虚拟化测试客户', orderDate: '2026-08-10', orderNo: `PO-${number}`, orderStatus: 4, printCount: 1, printStatus: 1, uuid: `virtual-order-${number}` }
}
