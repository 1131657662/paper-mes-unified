import { describe, expect, it } from 'vitest'
import type { FinishedProductRow } from './finishedProductRows'
import { buildCustomerFinishedProductRows } from './customerFinishedProductRows'

describe('客户成品规格汇总', () => {
  it('忽略直径和纸芯并合并相同门幅的成品', () => {
    const rows = [finishedRow('finish-1', 1000, 3), finishedRow('finish-2', 1200, 6)]

    const result = buildCustomerFinishedProductRows(rows)

    expect(result).toHaveLength(1)
    expect(result[0]).toMatchObject({ count: 2, weight: 1900, width: 900 })
  })
})

function finishedRow(uuid: string, diameter: number, coreDiameter: number): FinishedProductRow {
  return {
    key: uuid,
    sources: [],
    finish: {
      uuid,
      paperName: '白卡',
      gramWeight: 300,
      finishWidth: 900,
      finishDiameter: diameter,
      finishCoreDiameter: coreDiameter,
      actualWeight: 950,
    },
  }
}
