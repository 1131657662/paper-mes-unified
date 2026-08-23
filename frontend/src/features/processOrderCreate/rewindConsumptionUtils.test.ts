import { describe, expect, it } from 'vitest'
import { consumptionSources, effectiveConsumptionRatios, sourceUsageRows } from './rewindConsumptionUtils'

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

  it('历史空比例只占用显式比例后的剩余份额一次', () => {
    const first = { originalUuid: 'roll-1', consumeRatio: 50 }
    const legacy = { originalUuid: 'roll-1' }
    const duplicateLegacy = { originalUuid: 'roll-1' }
    const segments = [
      { sources: [first] },
      { sources: [legacy] },
      { sources: [duplicateLegacy] },
    ]

    const effective = effectiveConsumptionRatios(segments)
    const rows = sourceUsageRows(segments, [{
      value: 'roll-1', label: '母卷1', weight: 1000, weightKnown: true,
      roll: { localId: 'roll-local-1', uuid: 'roll-1', paperName: '纸', gramWeight: 1, originalWidth: 1000 },
    }])

    expect([effective.get(first), effective.get(legacy), effective.get(duplicateLegacy)]).toEqual([50, 50, 0])
    expect(rows).toEqual([expect.objectContaining({ consumeRatio: 100, consumeWeight: 1000, status: 'ok' })])
  })

  it('显式比例超过100%时保留错误总量用于提示', () => {
    const segments = [
      { sources: [{ originalUuid: 'roll-1', consumeRatio: 60 }] },
      { sources: [{ originalUuid: 'roll-1', consumeRatio: 60 }] },
    ]

    const rows = sourceUsageRows(segments, [
      {
        value: 'roll-1', label: '母卷1', weight: 1000, weightKnown: true,
        roll: { localId: 'roll-local-1', uuid: 'roll-1', paperName: '纸', gramWeight: 1, originalWidth: 1000 },
      },
    ])

    expect(rows).toEqual([expect.objectContaining({
      consumeRatio: 120,
      remainingRatio: -20,
      consumeWeight: 1200,
      status: 'error',
    })])
  })
})
