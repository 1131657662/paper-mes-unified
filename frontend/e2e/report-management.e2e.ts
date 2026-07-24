import { expect, test, type Page } from '@playwright/test'
import { hasE2eCredentials, signIn } from './auth'

test.describe('报表管理工作台', () => {
  test.skip(!hasE2eCredentials(), '设置报表 E2E 账号后运行')

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize({ width: 1366, height: 768 })
    await signIn(page)
  })

  test('订阅、预警与规则使用真实管理视图', async ({ page }) => {
    const errors = capturePageErrors(page)
    await page.goto('/reports/management/subscriptions')

    await expect(page.getByRole('heading', { name: '订阅与预警' })).toBeVisible()
    await expect(page.locator('.report-management__summary .ant-statistic')).toHaveCount(4)
    await page.getByRole('tab', { name: /预警事件/ }).click()
    await expect(page.getByRole('radiogroup', { name: '预警事件状态' })).toBeVisible()
    await page.getByRole('tab', { name: /阈值规则/ }).click()
    await expect(page.getByRole('button', { name: '新建规则' })).toBeVisible()
    await expectNoHorizontalOverflow(page)
    expect(errors).toEqual([])
  })

  test('指标口径页支持历史发布包审计', async ({ page }) => {
    const errors = capturePageErrors(page)
    await page.goto('/reports/management/metrics')

    await expect(page.getByRole('heading', { name: '指标口径与版本审计' })).toBeVisible()
    await expect(page.locator('.report-metric-release-item')).toHaveCount(2)
    await expect(page.locator('.report-metric-version-table')).toBeVisible()
    await expectNoHorizontalOverflow(page)
    expect(errors).toEqual([])
  })
})

function capturePageErrors(page: Page): string[] {
  const errors: string[] = []
  page.on('console', (message) => {
    if (message.type() === 'error' && !isKnownAntdWarning(message.text())) errors.push(message.text())
  })
  page.on('pageerror', (error) => errors.push(error.message))
  return errors
}

function isKnownAntdWarning(message: string): boolean {
  return message.includes('findDOMNode is deprecated') || message.includes('is deprecated in StrictMode')
}

async function expectNoHorizontalOverflow(page: Page): Promise<void> {
  const overflow = await page.evaluate(() => ({
    clientWidth: document.documentElement.clientWidth,
    scrollWidth: document.documentElement.scrollWidth,
  }))
  expect(overflow.scrollWidth).toBeLessThanOrEqual(overflow.clientWidth + 1)
}
