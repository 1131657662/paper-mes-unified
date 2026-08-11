import { expect, test, type Page, type Route } from '@playwright/test'

const PAGE_RECORD_COUNT = 50
const TOTAL_ORDER_COUNT = 59
const CPU_THROTTLE_RATE = 4
const FRAME_SAMPLE_COUNT = 60
const FRAME_P95_REVIEW_MS = 120

test('加工单页面在 pageSize=50 时仅渲染可视行并保留操作列', async ({ page }) => {
  await mockProcessOrderPage(page)

  await page.goto('/process-orders?size=50')

  await expect(page.getByRole('main')).toBeVisible()
  await expect(page.getByRole('heading', { name: '加工单' })).toBeVisible()
  await expect(page.getByText('PO-0001')).toBeVisible()
  await expect(page.getByText('共 59 条')).toBeVisible()

  const renderedRows = page.locator(
    '.process-order-table .ant-table-tbody-virtual-holder-inner > .ant-table-row',
  )
  await expect.poll(() => renderedRows.count()).toBeGreaterThan(0)
  expect(await renderedRows.count()).toBeLessThan(50)

  const tableBody = page.locator('.process-order-table .ant-table-tbody-virtual-holder')
  await expect(tableBody).toBeVisible()
  const bodyBox = await tableBody.boundingBox()
  expect(bodyBox?.height ?? 0).toBeGreaterThan(0)
  expect(bodyBox?.height ?? 0).toBeLessThan(700)

  const actionsHeader = page.getByRole('columnheader', { name: '操作' })
  await expect(actionsHeader).toBeVisible()
  expect(await actionsHeader.evaluate((element) => getComputedStyle(element).position)).toBe('sticky')
  await expect(renderedRows.first().getByRole('button', { name: '创建出库' })).toBeVisible()
})

test('加工单关键控件支持键盘焦点和标签菜单操作', async ({ page }) => {
  await mockProcessOrderPage(page)

  await page.goto('/process-orders')

  await expect(page.getByRole('main')).toBeVisible()
  await expect(page.getByRole('heading', { name: '加工单' })).toBeFocused()

  await page.keyboard.press('Tab')
  await expect(page.getByRole('textbox', { name: '关键字' })).toBeFocused()

  const tabActions = page.getByRole('button', { name: '标签操作' })
  await tabActions.focus()
  await page.keyboard.press('Enter')
  await expect(page.getByRole('menuitem', { name: '刷新当前' })).toBeVisible()

  await page.keyboard.press('Escape')
  await expect(page.getByRole('menuitem', { name: '刷新当前' })).toBeHidden()
  await expect(tabActions).toBeFocused()
})

test('记录加工单虚拟表格四倍 CPU 降速滚动基线', async ({ context, page }, testInfo) => {
  test.slow()
  await mockProcessOrderPage(page)
  await page.goto('/process-orders?size=50')

  const tableBody = page.locator('.process-order-table .ant-table-tbody-virtual-holder')
  await expect(tableBody).toBeVisible()
  const cdp = await context.newCDPSession(page)

  try {
    await cdp.send('Emulation.setCPUThrottlingRate', { rate: CPU_THROTTLE_RATE })
    const metrics = await measureVirtualScroll(page)
    await testInfo.attach('virtual-scroll-metrics.json', {
      body: Buffer.from(JSON.stringify(metrics, null, 2)),
      contentType: 'application/json',
    })
    console.info('[virtual-scroll-metrics]', JSON.stringify({
      project: testInfo.project.name,
      reviewRequired: metrics.p95FrameGapMs > FRAME_P95_REVIEW_MS,
      reviewThresholdMs: FRAME_P95_REVIEW_MS,
      ...metrics,
    }))

    expect(metrics.scrollTop).toBeGreaterThan(0)
    expect(metrics.renderedRows).toBeLessThan(PAGE_RECORD_COUNT)
  } finally {
    await cdp.send('Emulation.setCPUThrottlingRate', { rate: 1 })
    await cdp.detach()
  }
})

async function measureVirtualScroll(page: Page) {
  return page.evaluate(async ({ cpuThrottleRate, sampleCount }) => {
    const holder = document.querySelector<HTMLElement>('.ant-table-tbody-virtual-holder')
    if (!holder) throw new Error('Virtual table scroll holder is missing')

    const frameTimes: number[] = []
    const maxScrollTop = holder.scrollHeight - holder.clientHeight
    await new Promise<void>((resolve) => {
      const sample = (now: number) => {
        frameTimes.push(now)
        holder.scrollTop = maxScrollTop * (frameTimes.length / sampleCount)
        if (frameTimes.length >= sampleCount) requestAnimationFrame(() => resolve())
        else requestAnimationFrame(sample)
      }
      requestAnimationFrame(sample)
    })

    const gaps = frameTimes.slice(1).map((time, index) => time - frameTimes[index]).sort((a, b) => a - b)
    const p95Index = Math.max(0, Math.ceil(gaps.length * 0.95) - 1)
    return {
      cpuThrottleRate,
      domElements: document.getElementsByTagName('*').length,
      p95FrameGapMs: gaps[p95Index] ?? 0,
      renderedRows: document.querySelectorAll('.ant-table-tbody .ant-table-row').length,
      sampleCount: gaps.length,
      scrollTop: holder.scrollTop,
    }
  }, { cpuThrottleRate: CPU_THROTTLE_RATE, sampleCount: FRAME_SAMPLE_COUNT })
}

async function mockProcessOrderPage(page: Page): Promise<void> {
  await page.route('**/api/**', (route) => fulfillApi(route))
}

async function fulfillApi(route: Route): Promise<void> {
  const pathname = new URL(route.request().url()).pathname
  if (!pathname.startsWith('/api/')) return route.fallback()
  if (pathname === '/api/auth/me') {
    return fulfill(route, {
      permissions: [],
      realName: 'E2E Admin',
      roleCode: 'admin',
      username: 'e2e-admin',
      uuid: 'e2e-admin',
    })
  }
  if (pathname === '/api/process-orders') {
    return fulfill(route, {
      current: 1,
      records: Array.from({ length: PAGE_RECORD_COUNT }, (_, index) => createProcessOrder(index)),
      size: 50,
      total: TOTAL_ORDER_COUNT,
    })
  }
  if (pathname === '/api/customers') return fulfill(route, { current: 1, records: [], size: 200, total: 0 })
  if (pathname === '/api/notifications') return fulfill(route, { items: [], unreadCount: 0 })
  if (pathname === '/api/export-tasks') return fulfill(route, { runningCount: 0, unacknowledgedCount: 0 })
  if (pathname === '/api/system/runtime/configs') return fulfill(route, [])
  if (pathname === '/api/export-tasks/events') {
    return route.fulfill({ status: 200, contentType: 'text/event-stream', body: '' })
  }
  return fulfill(route, {})
}

async function fulfill(route: Route, data: unknown): Promise<void> {
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ code: 200, data, message: 'OK' }),
  })
}

function createProcessOrder(index: number) {
  const number = String(index + 1).padStart(4, '0')
  return {
    customerName: '虚拟化测试客户',
    orderDate: '2026-08-10',
    orderNo: `PO-${number}`,
    orderStatus: 4,
    printCount: 1,
    printStatus: 1,
    uuid: `virtual-order-${number}`,
  }
}
