import { describe, expect, it } from 'vitest'
import { finishUuidsFromNavigationState } from './deliveryCreateNavigation'

describe('finishUuidsFromNavigationState', () => {
  it('去重并丢弃无效的路由状态值', () => {
    expect(finishUuidsFromNavigationState({
      finishUuids: ['finish-1', 'finish-1', '../unsafe', 2],
    })).toEqual(['finish-1'])
  })

  it('非预期路由状态返回空数组', () => {
    expect(finishUuidsFromNavigationState(null)).toEqual([])
    expect(finishUuidsFromNavigationState({ finishUuids: 'finish-1' })).toEqual([])
  })
})
