import { describe, expect, it } from 'vitest'
import { serviceEditorActionBlockedReason } from './serviceEditorGuard'

describe('已保存附加工艺操作保护', () => {
  it('编辑器 dirty 时阻止切换编辑项和删除', () => {
    const reason = serviceEditorActionBlockedReason({
      analysis: { createCount: 1, excludedCount: 0, targetUuids: ['roll-1'], updateCount: 0 },
      dirty: true,
      summary: '重新包装 · 按件',
    })

    expect(reason).toContain('请先保存或还原')
  })

  it('编辑器 clean 时允许操作已保存配置', () => {
    expect(serviceEditorActionBlockedReason()).toBeUndefined()
  })
})
