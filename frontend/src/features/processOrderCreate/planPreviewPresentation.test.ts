import { describe, expect, it } from 'vitest'
import type { PlanPreviewVO } from '../../types/processOrder'
import { planPreviewSaveStatus } from './planPreviewPresentation'

describe('加工方案预览保存状态', () => {
  it.each([
    { configured: false, ready: true, color: 'warning', label: '预览通过，待保存' },
    { configured: true, ready: true, color: 'success', label: '已保存，可进入预览确认' },
    { configured: true, ready: false, color: 'error', label: '需修正' },
  ] as const)('ready=$ready configured=$configured 时显示 $label', ({ configured, ready, color, label }) => {
    const preview: PlanPreviewVO = { ready }

    expect(planPreviewSaveStatus(preview, configured)).toEqual({ color, label })
  })

  it('没有预览结果时不显示保存状态', () => {
    expect(planPreviewSaveStatus(undefined, false)).toBeUndefined()
  })
})
