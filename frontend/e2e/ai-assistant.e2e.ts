import { expect, test } from '@playwright/test'
import { hasE2eCredentials, signIn } from './auth'

const aiE2eEnabled = process.env.PAPER_MES_E2E_AI_ENABLED === 'true'

test.describe('智能助手 FAQ_ONLY', () => {
  test.skip(!hasE2eCredentials(), '设置 PAPER_MES_E2E_USERNAME 和 PAPER_MES_E2E_PASSWORD 后运行')
  test.skip(!aiE2eEnabled, '测试环境部署并启用 FAQ_ONLY 后设置 PAPER_MES_E2E_AI_ENABLED=true')

  test.beforeEach(async ({ page }) => {
    await signIn(page)
  })

  test('已授权用户可以获取带规则依据的只读回答', async ({ page }) => {
    await page.goto('/dashboard')
    await page.getByRole('button', { name: '智能助手' }).click()
    await expect(page.getByText('当前仅使用已审核本地规则')).toBeVisible()

    await page.getByPlaceholder('例如：E001 为什么不能操作？').fill('E001 为什么不能操作？')
    await page.getByRole('button', { name: /发送问题/ }).click()

    await expect(page.getByText(/当前操作被加工单状态机拦截/)).toBeVisible()
    await expect(page.getByText(/依据：E001-STATUS-GUARD v1.0.0/)).toBeVisible()
    await expect(page.getByText(/不执行任何业务操作/)).toBeVisible()
  })

  test('关闭后重新打开会销毁上一会话', async ({ page }) => {
    await page.goto('/dashboard')
    await page.getByRole('button', { name: '智能助手' }).click()
    await page.getByPlaceholder('例如：E001 为什么不能操作？').fill('E001')
    await page.getByRole('button', { name: /发送问题/ }).click()
    await expect(page.getByText(/依据：E001-STATUS-GUARD/)).toBeVisible()

    await page.getByRole('button', { name: '关闭智能助手' }).click()
    await expect(page.getByRole('dialog', { name: '智能助手' })).toHaveCount(0)
    await page.getByRole('button', { name: '智能助手' }).click()

    await expect(page.getByText(/依据：E001-STATUS-GUARD/)).toHaveCount(0)
    await expect(page.getByText('输入错误码或业务规则问题')).toBeVisible()
  })
})
