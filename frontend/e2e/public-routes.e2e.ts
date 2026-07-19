import { expect, test } from '@playwright/test'

test('未登录访问业务页时跳转登录并保留目标地址', async ({ page }) => {
  await page.goto('/reports')
  await expect(page).toHaveURL(/\/login/)
  await expect(page.getByRole('main')).toContainText('登录')
})

test('未知登录前路由不会暴露业务页面', async ({ page }) => {
  await page.goto('/settle-orders/create')
  await expect(page).toHaveURL(/\/login/)
  await expect(page.getByPlaceholder('请输入用户名')).toBeVisible()
})
