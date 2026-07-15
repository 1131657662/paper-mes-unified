import { describe, expect, it } from 'vitest'
import { DEFAULT_PAGE_TAB_PATH, createTab, ensurePageTabs } from './pageTabModel'

describe('pageTabModel', () => {
  it('空标签恢复到默认页', () => {
    expect(ensurePageTabs([])).toEqual([createTab(DEFAULT_PAGE_TAB_PATH)])
  })

  it('运行期间最多保留 12 个最近标签', () => {
    const tabs = Array.from({ length: 15 }, (_, index) => createTab(`/process-orders/order-${index}`))
    const result = ensurePageTabs(tabs)
    expect(result).toHaveLength(12)
    expect(result.at(-1)?.path).toBe('/process-orders/order-14')
  })

  it('重复打开同一路径只保留一个并移动到末尾', () => {
    const result = ensurePageTabs([createTab('/customers'), createTab('/papers'), createTab('/customers')])
    expect(result.map((item) => item.path)).toEqual(['/papers', '/customers'])
  })
})
