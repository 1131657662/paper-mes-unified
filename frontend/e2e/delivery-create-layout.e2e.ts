import { expect, test } from '@playwright/test'
import { hasE2eCredentials, signIn } from './auth'

test.describe('新建出库单冻结操作区', () => {
  test.skip(!hasE2eCredentials(), '设置 PAPER_MES_E2E_USERNAME 和 PAPER_MES_E2E_PASSWORD 后运行')
  test.use({ viewport: { width: 1366, height: 768 } })

  test.beforeEach(async ({ page }) => {
    await signIn(page)
    await page.goto('/delivery-orders/create')
  })

  test('内容区滚动到底部时操作区保持可见且不覆盖表格', async ({ page }) => {
    const scrollRegion = page.locator('.delivery-create-page__scroll')
    const footer = page.getByRole('contentinfo', { name: '出库单提交操作' })
    await expect(scrollRegion).toBeVisible()
    await expect(footer).toBeVisible()

    const before = await footer.boundingBox()
    await scrollRegion.evaluate((element) => { element.scrollTop = element.scrollHeight })
    await expect.poll(() => scrollRegion.evaluate((element) => element.scrollTop)).toBeGreaterThan(0)

    const after = await footer.boundingBox()
    const region = await scrollRegion.boundingBox()
    expect(before).not.toBeNull()
    expect(after).not.toBeNull()
    expect(region).not.toBeNull()
    expect(Math.abs(after!.y - before!.y)).toBeLessThan(1)
    expect(after!.y).toBeGreaterThanOrEqual(region!.y + region!.height - 1)
    expect(after!.y + after!.height).toBeLessThanOrEqual(768)
  })
})
