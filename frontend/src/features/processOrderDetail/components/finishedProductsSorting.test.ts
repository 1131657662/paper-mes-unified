import { describe, expect, it } from 'vitest'
import {
  sortCustomerComparisonRows,
  sortCustomerSummaryRows,
  sortPhysicalItemRows,
  updateCustomerSummarySortChain,
  type CustomerSpecificationComparisonRow,
} from './finishedProductsSorting'
import type { CustomerSpecificationGroup } from '../../processOrderCustomerSpec/customerSpecModel'
import type { FinishCustomerSpec } from '../../processOrderCustomerSpec/customerSpecTypes'
import type { FinishedProductRow } from './finishedProductRows'

describe('finishedProductsSorting', () => {
  it('客户规格汇总支持多列联动排序', () => {
    const first = updateCustomerSummarySortChain([], { field: 'paperName', columnKey: 'paperName', order: 'ascend' })
    const chain = updateCustomerSummarySortChain(first, [
      { field: 'paperName', columnKey: 'paperName', order: 'ascend' },
      { field: 'count', columnKey: 'count', order: 'descend' },
    ])
    const rows = [customerGroup('纸', 1), customerGroup('纸', 3), customerGroup('卡', 9)]

    expect(sortCustomerSummaryRows(rows, chain).map((row) => row.count)).toEqual([9, 3, 1])
  })

  it('客户逐件按客户规格字段排序', () => {
    const rows = [comparison('A-2', 1200), comparison('A-1', 1000)]
    const sorted = sortCustomerComparisonRows(rows, [{ field: 'customerSpecification', direction: 'asc' }])

    expect(sorted.map((row) => row.spec.finishRollNo)).toEqual(['A-1', 'A-2'])
  })

  it('实物逐件按差异排序', () => {
    const rows = [physical('A-1', 100, 110), physical('A-2', 100, 90)]
    const sorted = sortPhysicalItemRows(rows, [{ field: 'difference', direction: 'desc' }])

    expect(sorted.map((row) => row.finish.finishRollNo)).toEqual(['A-1', 'A-2'])
  })
})

function customerGroup(paperName: string, count: number): CustomerSpecificationGroup {
  return { key: `${paperName}-${count}`, paperName, count, weight: count, physicalSpecifications: [] }
}

function comparison(finishRollNo: string, width: number): CustomerSpecificationComparisonRow {
  return { row: physical(finishRollNo, 0, 0), spec: spec(finishRollNo, width) }
}

function spec(finishRollNo: string, width: number): FinishCustomerSpec {
  return { finishUuid: finishRollNo, finishRollNo, customerFinishWidth: width, finishVersion: 1, calculationMode: 'KEEP', specificationChanged: false, weightChanged: false, valid: true }
}

function physical(finishRollNo: string, estimateWeight: number, actualWeight: number): FinishedProductRow {
  return { key: finishRollNo, finish: { uuid: finishRollNo, finishRollNo, estimateWeight, actualWeight }, sources: [] }
}
