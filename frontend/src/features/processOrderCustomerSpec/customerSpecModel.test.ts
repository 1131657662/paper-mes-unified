import { describe, expect, it } from 'vitest'
import type { FinishedProductRow } from '../processOrderDetail/components/finishedProductRows'
import { buildCustomerSpecificationGroups } from './customerSpecModel'
import type { FinishCustomerSpec } from './customerSpecTypes'

describe('buildCustomerSpecificationGroups', () => {
  it('按客户规格归并不同的实物门幅', () => {
    const groups = buildCustomerSpecificationGroups(
      [row('f1', 500, 1109), row('f2', 502, 1113)],
      [spec('f1', 500, 1188), spec('f2', 500, 1190)],
    )
    expect(groups).toHaveLength(1)
    expect(groups[0]).toMatchObject({ paperName: '食品卡', gramWeight: 75, width: 500, count: 2, weight: 2378 })
    expect(groups[0]!.physicalSpecifications).toEqual(['白卡 / 70g / 500mm', '白卡 / 70g / 502mm'])
  })
})

function row(uuid: string, width: number, weight: number): FinishedProductRow {
  return { key: uuid, sources: [], finish: { uuid, paperName: '白卡', gramWeight: 70, finishWidth: width, actualWeight: weight } }
}

function spec(finishUuid: string, width: number, weight: number): FinishCustomerSpec {
  return {
    finishUuid, finishVersion: 1, calculationMode: 'KEEP', valid: true,
    physicalPaperName: '白卡', physicalGramWeight: 70, physicalFinishWidth: width,
    customerPaperName: '食品卡', customerGramWeight: 75, customerFinishWidth: 500,
    customerDisplayWeight: weight, specificationChanged: true, weightChanged: true,
  }
}
