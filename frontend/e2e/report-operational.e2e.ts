import { expect, test, type Page } from '@playwright/test'
import { hasE2eCredentials, signIn } from './auth'

interface TopicCase {
  label: string
  path: string
  inventoryScopeNote?: boolean
}

const PERIOD = '?dateFrom=2026-01-01&dateTo=2026-07-21'
const TOPICS: TopicCase[] = [
  { label: '结算分析', path: '/reports/settlement' + PERIOD },
  { label: '回款分析', path: '/reports/collection' + PERIOD },
  { label: '库存分析', path: '/reports/inventory' + PERIOD, inventoryScopeNote: true },
  { label: '出库分析', path: '/reports/delivery' + PERIOD },
]

test.describe('运营专题报表', () => {
  test.skip(!hasE2eCredentials(), '设置 PAPER_MES_E2E_USERNAME 和 PAPER_MES_E2E_PASSWORD 后运行')

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize({ width: 1366, height: 768 })
    await signIn(page)
  })

  for (const topic of TOPICS) {
    test(`${topic.label}完成数据与布局验收`, async ({ page }) => {
      const errors = capturePageErrors(page)

      await page.goto(topic.path)
      await expect(page.locator('.report-filter-card .ant-card-head-title')).toHaveText(topic.label)
      await expect(page.locator('.report-topic-metric')).toHaveCount(5)
      await expect(page.locator('.report-topic-trend')).toBeVisible()
      await expect(page.locator('.report-topic-breakdown')).toBeVisible()
      await expect(page.getByText(/数据截至/)).toBeVisible()
      await expect(page.locator('.ant-spin-spinning')).toHaveCount(0)
      await assertInventoryScope(page, topic)
      await expectNoHorizontalOverflow(page)
      expect(errors).toEqual([])
    })
  }
})

function capturePageErrors(page: Page): string[] {
  const errors: string[] = []
  page.on('console', (message) => {
    if (message.type() === 'error' && !isKnownAntdWarning(message.text())) errors.push(message.text())
  })
  page.on('pageerror', (error) => errors.push(error.message))
  return errors
}

function isKnownAntdWarning(message: string): boolean {
  return message.includes('findDOMNode is deprecated') || message.includes('is deprecated in StrictMode')
}

async function assertInventoryScope(page: Page, topic: TopicCase): Promise<void> {
  const note = page.getByText('当前库存快照', { exact: true })
  if (topic.inventoryScopeNote) {
    await expect(note).toBeVisible()
    await expect(page.getByText(/不代表历史月末库存余额/)).toBeVisible()
    return
  }
  await expect(note).toHaveCount(0)
}

async function expectNoHorizontalOverflow(page: Page): Promise<void> {
  const overflow = await page.evaluate(() => ({
    clientWidth: document.documentElement.clientWidth,
    scrollWidth: document.documentElement.scrollWidth,
  }))
  expect(overflow.scrollWidth).toBeLessThanOrEqual(overflow.clientWidth + 1)
}
