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
    { name: '出库列表', path: '/delivery-orders' },
    { name: '结算列表', path: '/settle-orders' },
    { name: '用户权限', path: '/users' },
    { name: '个人中心', path: '/profile' },
  ]) {
    test(`${route.name}没有 serious 或 critical 级无障碍问题`, async ({ page }) => {
      await page.goto(route.path)
      await expect(page.getByRole('main')).toBeVisible()

      await expectNoBlockingA11yViolations(page)
    })
  }

  for (const route of [
    { name: '出库管理', path: '/delivery-orders' },
    { name: '结算管理', path: '/settle-orders' },
    { name: '用户权限', path: '/users' },
  ]) {
    test(`${route.name}路由将焦点放在一级标题`, async ({ page }) => {
      await page.goto(route.path)
      const heading = page.getByRole('heading', { level: 1, name: route.name })

      await expect(heading).toBeVisible()
      await expect(heading).toBeFocused()
    })
  }

  test('页面标签的关闭操作不会伪装成 tab 角色', async ({ page }) => {
    await page.goto('/process-orders')
    await expect(page.getByRole('main')).toBeVisible()

    const tabSemantics = await page.locator('[role="tablist"]').evaluateAll((tablists) => tablists.flatMap((tablist) =>
      [...tablist.querySelectorAll<HTMLElement>('[role="tab"]')].map((tab) => ({
        ariaSelected: tab.getAttribute('aria-selected'),
        tagName: tab.tagName,
      })),
    ))

    expect(tabSemantics.length).toBeGreaterThan(0)
    expect(tabSemantics.every(({ ariaSelected, tagName }) => ariaSelected !== null && tagName !== 'BUTTON')).toBe(true)
    await expect(page.locator('[role="tablist"] button[role="tab"]')).toHaveCount(0)
  })
})
