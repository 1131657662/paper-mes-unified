import { expect, test, type Page } from '@playwright/test'
import { hasLimitedE2eCredentials, signIn } from './auth'

const PERIOD = { dateFrom: '2026-01-01', dateTo: '2026-07-21' }

test.describe('报表金额与导出权限', () => {
  test.skip(!hasLimitedE2eCredentials(), '设置受限角色 E2E 账号后运行')

  test.beforeEach(async ({ page }) => {
    await signIn(page, {
      username: process.env.PAPER_MES_E2E_LIMITED_USERNAME,
      password: process.env.PAPER_MES_E2E_LIMITED_PASSWORD,
    })
  })

  test('无结算读取权限时后端脱敏且导出不可绕过', async ({ page }) => {
    const analysisResponse = page.waitForResponse((response) =>
      response.url().includes('/api/reports/topics/settlement/query'))
    await page.goto(`/reports/settlement?dateFrom=${PERIOD.dateFrom}&dateTo=${PERIOD.dateTo}`)

    const analysis = await analysisResponse
    expect(analysis.status()).toBe(200)
    const payload: unknown = await analysis.json()
    expect(payload).toMatchObject({ data: { overview: {
      overdueDocuments: 0,
      partialDocuments: 0,
      pendingDocuments: 0,
      totalDocuments: 0,
    } } })
    expect(JSON.stringify(payload)).not.toMatch(/"(?:overdue|received|total|unreceived)Amount":/)

    await expect(amountMetric(page, '应收金额')).toHaveText('-')
    await expect(amountMetric(page, '已结清金额')).toHaveText('-')
    await expect(amountMetric(page, '未收余额')).toHaveText('-')
    await expect(page.getByRole('button', { name: '导出', exact: true })).toHaveCount(0)
    await expect(page.locator('body')).not.toContainText('undefined')
    await expectNoHorizontalOverflow(page)

    const exportResponse = await page.request.post('/api/export-tasks/reports', { data: {
      query: PERIOD,
      reportPath: '/reports/settlement',
      requestId: `limited-report-${Date.now()}`,
    } })
    expect(exportResponse.status()).toBe(403)
  })
})

function amountMetric(page: Page, label: string) {
  return page.locator('.report-topic-metric').filter({ hasText: label }).locator('strong')
}

async function expectNoHorizontalOverflow(page: Page): Promise<void> {
  const overflow = await page.evaluate(() => ({
    clientWidth: document.documentElement.clientWidth,
    scrollWidth: document.documentElement.scrollWidth,
  }))
  expect(overflow.scrollWidth).toBeLessThanOrEqual(overflow.clientWidth + 1)
}
