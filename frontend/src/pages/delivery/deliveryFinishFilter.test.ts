import { describe, expect, it } from 'vitest'
import type { AvailableFinishVO } from '../../types/delivery'
import { defaultDeliveryFinishFilters, filterDeliveryFinishes } from './deliveryFinishFilter'
import { mergeAvailableFinishRows } from './deliverySelectionModel'

describe('filterDeliveryFinishes', () => {
  const missingSource = finish({ finishUuid: 'missing-source', sourceMotherRolls: [], sourceType: 1 })
  const missingIdentity = finish({ finishUuid: 'missing-id', sourceMotherRolls: [{ originalUuid: 'original-1', paperName: '牛卡纸', rollNo: undefined }] })
  const healthy = finish({ finishUuid: 'healthy', sourceMotherRolls: [{ originalUuid: 'original-2', rollNo: 'M-001', paperName: '白卡纸' }] })
  const direct = finish({ finishUuid: 'direct', sourceMotherRolls: [], sourceType: 2 })

  it('直发母卷不判定为来源缺失', () => {
    const rows = filterDeliveryFinishes([missingSource, direct], { ...defaultDeliveryFinishFilters, sourceIssue: 'missingSource' })
    expect(rows.map((item) => item.finishUuid)).toEqual(['missing-source'])
  })

  it('可按来源卷号和品名搜索', () => {
    expect(filterDeliveryFinishes([missingIdentity, healthy], { ...defaultDeliveryFinishFilters, keyword: 'M001' }))
      .toEqual([healthy])
    expect(filterDeliveryFinishes([missingIdentity, healthy], { ...defaultDeliveryFinishFilters, keyword: '牛卡' }))
      .toEqual([missingIdentity])
  })

  it('仅显示已选项时按成品 UUID 过滤', () => {
    const rows = filterDeliveryFinishes([healthy, direct], { ...defaultDeliveryFinishFilters, selectedOnly: true }, ['direct'])
    expect(rows).toEqual([direct])
  })

  it('分页切换时保留之前页已选卷并移除取消项', () => {
    const first = finish({ finishUuid: 'first' })
    const second = finish({ finishUuid: 'second' })

    const merged = mergeAvailableFinishRows({ first }, ['first', 'second'], [second])
    const afterClear = mergeAvailableFinishRows(merged, ['second'], [])

    expect(Object.keys(merged)).toEqual(['first', 'second'])
    expect(afterClear).toEqual({ second })
  })
})

function finish(values: Partial<AvailableFinishVO>): AvailableFinishVO {
  return { finishRollNo: values.finishUuid ?? '卷号', sourceType: 1, ...values } as AvailableFinishVO
}
