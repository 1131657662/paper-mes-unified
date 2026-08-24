import { expect, test } from '@playwright/test'
import { signIn } from './auth'

test.describe('test 环境真实页面审查', () => {
  test('采集 AI 页面和新建流程控件', async ({ page }, testInfo) => {
    test.setTimeout(90_000)
    await signIn(page)

    await page.goto('/ai-memory-review')
    await expect(page.getByRole('heading', { name: 'AI记忆审核' })).toBeVisible()
    await page.screenshot({ path: testInfo.outputPath('ai-memory-review-desktop.png'), fullPage: true })
    await page.setViewportSize({ width: 390, height: 844 })
    await page.screenshot({ path: testInfo.outputPath('ai-memory-review-mobile.png'), fullPage: true })

    await page.setViewportSize({ width: 1366, height: 768 })
    await page.goto('/dashboard')
    await page.getByRole('button', { name: '智能助手' }).click()
    await expect(page.getByRole('dialog', { name: '智能助手' })).toBeVisible()
    await page.screenshot({ path: testInfo.outputPath('global-ai-desktop.png'), fullPage: true })
    await page.setViewportSize({ width: 390, height: 844 })
    await page.screenshot({ path: testInfo.outputPath('global-ai-mobile.png'), fullPage: true })

    await page.setViewportSize({ width: 1366, height: 768 })
    await page.goto('/process-orders/create')
    await page.waitForLoadState('domcontentloaded')
    await page.waitForTimeout(2_000)
    await page.screenshot({ path: testInfo.outputPath('process-create-step1.png'), fullPage: true })
    const controls = await page.locator('input, textarea, button, [role="combobox"]').evaluateAll((nodes) =>
      nodes.map((node) => ({
        aria: node.getAttribute('aria-label'),
        placeholder: node.getAttribute('placeholder'),
        text: node.textContent?.trim(),
        type: node.getAttribute('type'),
      })))
    console.log(`[audit-controls] ${JSON.stringify(controls)}`)
  })
})
