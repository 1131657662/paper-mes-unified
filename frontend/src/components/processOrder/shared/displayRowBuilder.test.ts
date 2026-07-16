import { describe, expect, it } from 'vitest'
import type { FinishProductionVO, RollProductionVO } from '../../../types/processOrder'
import { buildDisplayRows } from './displayRowBuilder'

describe('母卷加工产出排序', () => {
  it('按母卷规格排序并在母卷内按成品规格排序', () => {
    const rows = buildDisplayRows([
      production('roll-2', 240, [finish('finish-2b', 992), finish('finish-2a', 878)]),
      production('roll-1', 220, [finish('finish-1b', 992), finish('finish-1a', 878)]),
    ])

    expect(rows.map((row) => row.mainProduction.originalUuid)).toEqual(['roll-1', 'roll-2'])
    expect(rows[0]?.finishes.map((finishItem) => finishItem.uuid)).toEqual(['finish-1a', 'finish-1b'])
  })
})

function production(originalUuid: string, gramWeight: number, finishes: FinishProductionVO[]): RollProductionVO {
  return {
    originalUuid,
    paperName: '瑞典恩索涂布牛卡纸',
    gramWeight,
    originalWidth: 1200,
    rollWeight: 1000,
    finishes,
  }
}

function finish(uuid: string, finishWidth: number): FinishProductionVO {
  return {
    uuid,
    paperName: '瑞典恩索涂布牛卡纸',
    gramWeight: 220,
    finishWidth,
    actualWeight: 500,
  }
}
