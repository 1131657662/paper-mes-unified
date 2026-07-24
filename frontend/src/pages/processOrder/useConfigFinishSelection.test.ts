import { describe, expect, it } from 'vitest'
import { addDirtyUuids, removeDirtyUuids } from './useConfigFinishSelection'

describe('成品配置按母卷追踪未保存状态', () => {
  it('复制配置时保留所有目标母卷的 dirty 标记', () => {
    expect(addDirtyUuids(['roll-1'], ['roll-2', 'roll-3'])).toEqual(['roll-1', 'roll-2', 'roll-3'])
  })

  it('保存当前母卷时只清除该母卷的 dirty 标记', () => {
    expect(removeDirtyUuids(['roll-1', 'roll-2'], ['roll-1'])).toEqual(['roll-2'])
  })
})
