import { describe, expect, it } from 'vitest'
import type { RollProductionVO } from '../../../types/processOrder'
import {
  buildFinishedProductRows,
  calculateFinishedProductTotals,
} from './finishedProductRows'

describe('成品重量汇总', () => {
  it('历史预估值不闭合时按母卷总重重新分配', () => {
    const production: RollProductionVO = {
      originalUuid: 'roll-legacy',
      originalWidth: 2400,
      actualWeight: 1862,
      mainStepType: 1,
      finishes: [
        finish('a', 621),
        finish('b', 621),
        finish('c', 621),
      ],
    }

    const rows = buildFinishedProductRows([production])

    expect(rows.map((row) => row.estimateWeight)).toEqual([621, 621, 620])
    expect(calculateFinishedProductTotals(rows).estimateWeight).toBe(1862)
  })
})

function finish(uuid: string, estimateWeight: number) {
  return { uuid, finishWidth: 800, estimateWeight }
}
