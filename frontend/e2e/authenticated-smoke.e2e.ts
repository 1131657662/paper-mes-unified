import { expect, test } from '@playwright/test'
import { hasE2eCredentials, signIn } from './auth'

test.describe('登录后核心页面冒烟', () => {
  test.skip(!hasE2eCredentials(), '设置 PAPER_MES_E2E_USERNAME 和 PAPER_MES_E2E_PASSWORD 后运行')

  test.beforeEach(async ({ page }) => {
    await signIn(page)
  })

  test('仪表盘可以加载核心工作区', async ({ page }) => {
    await page.goto('/dashboard')
    await expect(page.getByRole('main')).toBeVisible()
    await expect(page.locator('.dashboard-panel').first()).toBeVisible()
  })

  test('新建结算单显示归属日期和候选表', async ({ page }) => {
    await page.goto('/settle-orders/create')
    await expect(page.getByRole('heading', { name: '新建结算单' })).toBeVisible()
    await expect(page.getByText('归属日期范围', { exact: true })).toBeVisible()
    await expect(page.getByRole('columnheader', { name: '应收' })).toBeVisible()
  })

  test('报表显示统一归属口径', async ({ page }) => {
    await page.goto('/reports')
    await expect(page.getByText('统计周期（归属日期）', { exact: true })).toBeVisible()
    await expect(page.getByText(/优先使用回录完成日期/)).toBeVisible()
  })
})
