import { describe, expect, it } from 'vitest'
import type { FinishConfigSpecDTO } from '../../types/processOrder'
import { sawSpecificationRowKey } from './sawSpecificationRowKey'

describe('锯切规格表行标识', () => {
  it('规格输入更新为新对象后保持同一行标识', () => {
    const before: FinishConfigSpecDTO = { itemType: 'FINISH', finishWidth: 1000, count: 1 }
    const after: FinishConfigSpecDTO = { ...before, finishWidth: 100 }

    expect(sawSpecificationRowKey(before, 0)).toBe(sawSpecificationRowKey(after, 0))
  })

  it('相邻规格使用不同的行标识', () => {
    const spec: FinishConfigSpecDTO = { itemType: 'FINISH', finishWidth: 1000, count: 1 }

    expect(sawSpecificationRowKey(spec, 0)).not.toBe(sawSpecificationRowKey(spec, 1))
  })
})
