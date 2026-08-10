import AxeBuilder from '@axe-core/playwright'
import { expect, type Page } from '@playwright/test'

const BLOCKING_IMPACTS = new Set(['serious', 'critical'])
const WCAG_TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa']

export async function expectNoBlockingA11yViolations(page: Page): Promise<void> {
  await expect(page.locator('.ant-spin-spinning')).toHaveCount(0)
  const results = await new AxeBuilder({ page }).withTags(WCAG_TAGS).analyze()
  const violations = results.violations.filter(({ impact }) => impact && BLOCKING_IMPACTS.has(impact))

  expect(violations, formatViolations(violations)).toEqual([])
}

function formatViolations(violations: Array<{ help: string; id: string; nodes: unknown[] }>): string {
  if (violations.length === 0) return 'No serious or critical accessibility violations'
  return violations
    .map(({ help, id, nodes }) => `${id}: ${help} (${nodes.length} affected nodes)`)
    .join('\n')
}
