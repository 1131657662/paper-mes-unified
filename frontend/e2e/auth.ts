import type { Page } from '@playwright/test'

interface SignInCredentials {
  password?: string
  username?: string
}

export function hasE2eCredentials(): boolean {
  return Boolean(process.env.PAPER_MES_E2E_USERNAME && process.env.PAPER_MES_E2E_PASSWORD)
}

export function hasLimitedE2eCredentials(): boolean {
  return Boolean(process.env.PAPER_MES_E2E_LIMITED_USERNAME
    && process.env.PAPER_MES_E2E_LIMITED_PASSWORD)
}

export async function openLogin(page: Page): Promise<void> {
  const usernameField = page.getByPlaceholder('请输入用户名')
  for (let attempt = 0; attempt < 2; attempt += 1) {
    await page.goto('/login')
    try {
      await usernameField.waitFor({ state: 'visible', timeout: 10_000 })
      return
    } catch (error) {
      if (attempt === 1) throw error
    }
  }
}

export async function signIn(page: Page, credentials: SignInCredentials = {}): Promise<void> {
  const username = credentials.username ?? process.env.PAPER_MES_E2E_USERNAME
  const password = credentials.password ?? process.env.PAPER_MES_E2E_PASSWORD
  if (!username || !password) throw new Error('E2E credentials are not configured')
  await openLogin(page)
  await page.getByPlaceholder('请输入用户名').fill(username)
  await page.getByPlaceholder('请输入密码').fill(password)
  await page.getByRole('region', { name: '登录表单' })
    .getByRole('button', { name: /登录/ })
    .click()
  await page.waitForURL((url) => url.pathname !== '/login')
}
