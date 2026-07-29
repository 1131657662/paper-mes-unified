import { describe, expect, it } from 'vitest'
import type { OriginalRoll } from '../../types/processOrder'
import { routeBatchTargets } from './routeBatchTargets'

describe('链式工艺批量目标', () => {
  it('只包含同加工方式、同主工艺和同规格母卷', () => {
    const current = roll('current')
    const compatible = roll('compatible')
    const rolls = [
      current,
      compatible,
      roll('onsite', { processMode: 2 }),
      roll('saw', { mainStepType: 1 }),
      roll('paper', { paperName: '白卡纸' }),
      roll('gram', { gramWeight: 150 }),
      roll('width', { originalWidth: 900 }),
    ]

    const targets = routeBatchTargets(rolls, current)

    expect(targets).toEqual([compatible])
  })
})

function roll(uuid: string, overrides: Partial<OriginalRoll> = {}): OriginalRoll {
  return {
    uuid,
    processMode: 1,
    mainStepType: 2,
    paperName: '牛卡纸',
    gramWeight: 120,
    originalWidth: 1000,
    ...overrides,
  }
}
