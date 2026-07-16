import { describe, expect, it } from 'vitest'
import type { FinishProductionVO, RollProductionVO } from '../../../types/processOrder'
import { buildFinishedProductRows } from './finishedProductRows'

describe('母卷成品明细排序', () => {
  it('先排母卷再排列各母卷对应的成品', () => {
    const productions = [
      production({ originalUuid: 'roll-b', gramWeight: 250, originalWidth: 1200, finishes: [
        finish({ uuid: 'finish-b2', gramWeight: 250, finishWidth: 900, actualWeight: 600 }),
        finish({ uuid: 'finish-b1', gramWeight: 250, finishWidth: 800, actualWeight: 700 }),
      ] }),
      production({ originalUuid: 'roll-a', gramWeight: 200, originalWidth: 1300, finishes: [
        finish({ uuid: 'finish-a2', gramWeight: 200, finishWidth: 1000, actualWeight: 500 }),
        finish({ uuid: 'finish-a1', gramWeight: 200, finishWidth: 900, actualWeight: 600 }),
      ] }),
    ]

    const result = buildFinishedProductRows(productions)

    expect(result.map((row) => row.key)).toEqual([
      'finish-a1',
      'finish-a2',
      'finish-b1',
      'finish-b2',
    ])
  })
})

function production(options: {
  finishes: FinishProductionVO[]
  gramWeight: number
  originalUuid: string
  originalWidth: number
}): RollProductionVO {
  return { ...options, paperName: '白卡', rollWeight: 1000 }
}

function finish(options: {
  actualWeight: number
  finishWidth: number
  gramWeight: number
  uuid: string
}): FinishProductionVO {
  return { ...options, paperName: '白卡' }
}
