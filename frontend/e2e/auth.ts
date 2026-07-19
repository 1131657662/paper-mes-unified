import type { Page } from '@playwright/test'

export function hasE2eCredentials(): boolean {
  return Boolean(process.env.PAPER_MES_E2E_USERNAME && process.env.PAPER_MES_E2E_PASSWORD)
}

export async function signIn(page: Page): Promise<void> {
  const username = process.env.PAPER_MES_E2E_USERNAME
  const password = process.env.PAPER_MES_E2E_PASSWORD
  if (!username || !password) throw new Error('E2E credentials are not configured')
  await page.goto('/login')
  await page.getByPlaceholder('请输入用户名').fill(username)
  await page.getByPlaceholder('请输入密码').fill(password)
  await page.getByRole('region', { name: '登录表单' })
    .getByRole('button', { name: /登录/ })
    .click()
  await page.waitForURL((url) => url.pathname !== '/login')
}
