import { describe, expect, it } from 'vitest'
import { consumptionSources } from './rewindConsumptionUtils'

describe('合并复卷来源消耗', () => {
  it('切换到消耗比例模式时清除旧的重量分摊比例', () => {
    const sources = consumptionSources(
      ['roll-1', 'roll-2'],
      [{ originalUuid: 'roll-1', shareRatio: 100 }],
    )

    expect(sources).toEqual([
      { originalUuid: 'roll-1', shareRatio: undefined, consumeRatio: 100, sourceSort: 1 },
      { originalUuid: 'roll-2', shareRatio: undefined, consumeRatio: 100, sourceSort: 2 },
    ])
  })
})
