import { expect, test } from '@playwright/test'
import { expectNoBlockingA11yViolations } from './accessibility'
import { hasE2eCredentials, signIn } from './auth'

test('登录页没有 serious 或 critical 级无障碍问题', async ({ page }) => {
  await page.goto('/login')
  await expect(page.getByRole('main')).toBeVisible()

  await expectNoBlockingA11yViolations(page)
})

test.describe('登录后关键路径无障碍门禁', () => {
  test.skip(!hasE2eCredentials(), '未配置 E2E 凭据，登录后 axe 用例未执行')

  test.beforeEach(async ({ page }) => {
    await signIn(page)
  })

  for (const route of [
    { name: '仪表盘', path: '/dashboard' },
    { name: '加工单列表', path: '/process-orders' },
    { name: '个人中心', path: '/profile' },
  ]) {
    test(`${route.name}没有 serious 或 critical 级无障碍问题`, async ({ page }) => {
      await page.goto(route.path)
      await expect(page.getByRole('main')).toBeVisible()

      await expectNoBlockingA11yViolations(page)
    })
  }
})
