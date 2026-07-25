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

  it('来源关系缺失时从所属母卷补齐逐件展示字段', () => {
    const source = production({
      originalUuid: 'roll-a',
      gramWeight: 200,
      originalWidth: 1300,
      finishes: [finish({ uuid: 'finish-a1', gramWeight: 200, finishWidth: 900, actualWeight: 600 })],
    })
    source.extraNo = 'NO-1'
    source.rollNo = 'ROLL-1'
    source.pieceNum = 2

    const result = buildFinishedProductRows([source])[0]

    expect(result).toBeDefined()
    expect(result?.sources[0]).toMatchObject({
      originalUuid: 'roll-a', extraNo: 'NO-1', rollNo: 'ROLL-1', paperName: '白卡',
      gramWeight: 200, originalWidth: 1300, rollWeight: 1000, pieceNum: 2,
    })
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
