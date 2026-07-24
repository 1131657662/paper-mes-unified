import { describe, expect, it } from 'vitest'
import type { FinishedProductRow } from './finishedProductRows'
import {
  buildPhysicalSpecificationGroups,
  calculatePhysicalSpecificationTotals,
} from './physicalSpecificationModel'

describe('实物规格汇总', () => {
  it('相同实物规格和类型合并为一行', () => {
    const groups = buildPhysicalSpecificationGroups([
      row('one', { actualWeight: 100, estimateWeight: 98 }),
      row('two', { actualWeight: 120, estimateWeight: 119 }),
    ])

    expect(groups).toHaveLength(1)
    expect(groups[0]).toMatchObject({ count: 2, recordedCount: 2, actualWeight: 220, estimateWeight: 217, difference: 3 })
  })

  it('不同规格或成品类型分别汇总', () => {
    const groups = buildPhysicalSpecificationGroups([
      row('finish'),
      row('wide', { finishWidth: 1000 }),
      row('spare', { isSpare: 1 }),
      row('trim', { finishWidth: 3, isRemain: 1 }),
    ])

    expect(groups.map(({ productType }) => productType)).toEqual(['FINISH', 'FINISH', 'SPARE', 'TRIM'])
  })

  it('作废卷不进入实物汇总', () => {
    const groups = buildPhysicalSpecificationGroups([
      row('active'),
      row('voided', { rollNoStatus: 3 }),
    ])

    expect(groups).toHaveLength(1)
    expect(groups[0]?.count).toBe(1)
  })

  it('未全部回录时不显示误导性的重量差异', () => {
    const groups = buildPhysicalSpecificationGroups([
      row('recorded', { actualWeight: 100, estimateWeight: 98 }),
      row('pending', { estimateWeight: 102 }),
    ])

    expect(groups[0]).toMatchObject({ count: 2, recordedCount: 1, actualWeight: 100, estimateWeight: 200 })
    expect(groups[0]?.difference).toBeUndefined()
  })

  it('合计统计所有分组的件数和重量', () => {
    const groups = buildPhysicalSpecificationGroups([
      row('finish', { actualWeight: 100, estimateWeight: 98 }),
      row('trim', { actualWeight: 2, estimateWeight: 2, finishWidth: 3, isRemain: 1 }),
    ])

    expect(calculatePhysicalSpecificationTotals(groups)).toMatchObject({
      actualWeight: 102, count: 2, difference: 2, estimateWeight: 100, recordedCount: 2,
    })
  })
})

function row(uuid: string, finish: Partial<FinishedProductRow['finish']> = {}): FinishedProductRow {
  return {
    finish: { uuid, paperName: '白卡', gramWeight: 300, finishWidth: 1175, ...finish },
    key: uuid,
    sources: [],
  }
}
