import { expect, test, type Page, type Route } from '@playwright/test'

const PAGE_RECORD_COUNT = 50
const TOTAL_ORDER_COUNT = 59

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
