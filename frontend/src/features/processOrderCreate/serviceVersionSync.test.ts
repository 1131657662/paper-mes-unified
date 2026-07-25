import { describe, expect, it } from 'vitest'
import type { ProcessOrderDetailVO } from '../../types/processOrder'
import { freshDraftVersion } from './serviceVersionSync'

describe('附加工艺版本同步', () => {
  it('只接受服务端返回的更高版本', () => {
    expect(freshDraftVersion(detail(8), 7)).toBe(8)
    expect(freshDraftVersion(detail(7), 7)).toBeUndefined()
    expect(freshDraftVersion(detail(6), 7)).toBeUndefined()
  })

  it('缺少版本时拒绝解除写入阻塞', () => {
    expect(freshDraftVersion(detail(undefined), 7)).toBeUndefined()
    expect(freshDraftVersion(undefined, 7)).toBeUndefined()
  })
})

function detail(version?: number): ProcessOrderDetailVO {
  return {
    finishRolls: [],
    order: { uuid: 'order-1', version },
    originalRolls: [],
    rolls: [],
    steps: [],
  }
}
