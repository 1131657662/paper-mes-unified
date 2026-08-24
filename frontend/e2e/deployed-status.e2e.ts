import { test } from '@playwright/test'
import { signIn } from './auth'

test('test 环境 AI 状态探测', async ({ page }) => {
  test.skip(process.env.PAPER_MES_E2E_DEPLOYED_PROBES !== 'true', '仅在明确启用测试环境部署探测时运行')
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
