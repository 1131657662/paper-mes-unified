import { expect, test, type Page, type Route } from '@playwright/test'

test('管理员可以查看项目记忆并进入补丁和回滚操作', async ({ page }, testInfo) => {
  await mockProjectMemoryPage(page, 'admin')

  await page.goto('/project-memory')

  await expect(page.getByRole('heading', { name: '项目记忆' })).toBeVisible()
  await expect(page.getByText('1.0.2', { exact: true }).first()).toBeVisible()
  await expect(page.getByText('正常', { exact: true })).toBeVisible()
  await expect(page.locator('.project-memory-document pre')).toContainText('rule-memory-safety')

  await page.getByRole('tab', { name: '版本历史' }).click()
  await expect(page.getByRole('cell', { name: '1.0.1', exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: '回滚' })).toBeVisible()

  await page.getByRole('tab', { name: '提交补丁' }).click()
  await expect(page.getByRole('button', { name: '提交补丁' })).toBeVisible()
  await testInfo.attach('project-memory-admin.png', {
    body: await page.screenshot(),
    contentType: 'image/png',
  })
})

test('普通智能助手用户只能查看且窄屏无横向溢出', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await mockProjectMemoryPage(page, 'operator')

  await page.goto('/project-memory')

  await expect(page.getByRole('heading', { name: '项目记忆' })).toBeVisible()
  await expect(page.getByRole('tab', { name: '提交补丁' })).toHaveCount(0)
  await page.getByRole('tab', { name: '版本历史' }).click()
  await expect(page.getByRole('button', { name: '回滚' })).toHaveCount(0)
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
})

async function mockProjectMemoryPage(page: Page, roleCode: 'admin' | 'operator') {
  page.on('pageerror', (error) => console.error('[project-memory-page-error]', error))
  page.on('console', (message) => {
    if (message.type() === 'error') console.error('[project-memory-console-error]', message.text())
  })
  await page.route('**/api/**', (route) => fulfillApi(route, roleCode))
}

async function fulfillApi(route: Route, roleCode: 'admin' | 'operator'): Promise<void> {
  const pathname = new URL(route.request().url()).pathname
  if (!pathname.startsWith('/api/')) return route.fallback()
  if (pathname === '/api/auth/me') return fulfill(route, currentUser(roleCode))
  if (pathname === '/api/ai/project-memory/current') return fulfill(route, snapshot())
  if (pathname === '/api/ai/project-memory/versions') return fulfill(route, versions())
  if (pathname === '/api/notifications') return fulfill(route, { items: [], unreadCount: 0 })
  if (pathname === '/api/export-tasks') return fulfill(route, { runningCount: 0, unacknowledgedCount: 0 })
  if (pathname === '/api/system/runtime/configs') return fulfill(route, [])
  if (pathname === '/api/export-tasks/events') {
    return route.fulfill({ status: 200, contentType: 'text/event-stream', body: '' })
  }
  return fulfill(route, {})
}

function currentUser(roleCode: 'admin' | 'operator') {
  return {
    permissions: roleCode === 'admin' ? ['*'] : ['ai:assist'],
    realName: roleCode === 'admin' ? 'E2E Admin' : 'E2E Operator',
    roleCode,
    username: `e2e-${roleCode}`,
    uuid: `e2e-${roleCode}`,
  }
}

function snapshot() {
  return {
    checksum: `sha256:${'0'.repeat(64)}`,
    document: { memoryVersion: '1.0.2', rules: { 'rule-memory-safety': { status: 'ACTIVE' } } },
    memoryVersion: '1.0.2',
    schemaVersion: '1.0',
    state: 'READY',
  }
}

function versions() {
  return [
    version('1.0.2', 'ACTIVE', '当前规则'),
    version('1.0.1', 'SUPERSEDED', '现场确认'),
  ]
}

function version(memoryVersion: string, status: string, patchNotes: string) {
  return {
    approvedBy: 'admin', checksum: `sha256:${memoryVersion.padEnd(64, '0')}`,
    createdAt: '2026-08-15T20:00:00', createdBy: 'admin', memoryVersion,
    patchNotes, schemaVersion: '1.0', status,
  }
}

async function fulfill(route: Route, data: unknown): Promise<void> {
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ code: 200, data, message: 'OK' }),
  })
}
