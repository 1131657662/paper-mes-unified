import { test } from '@playwright/test'
import { signIn } from './auth'

test('test 环境 AI 状态探测', async ({ page }) => {
  await signIn(page)
  const values = await page.evaluate(async () => {
    const paths = ['/api/ai/status', '/api/ai/process-status', '/api/ai/provider-settings/deepseek']
    const result: Record<string, unknown> = {}
    for (const path of paths) {
      const response = await fetch(path)
      result[path] = { status: response.status, body: await response.json() }
    }
    return result
  })
  console.log(`[ai-status] ${JSON.stringify(values)}`)
})
