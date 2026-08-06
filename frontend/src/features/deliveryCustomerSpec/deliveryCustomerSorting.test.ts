import { describe, expect, it } from 'vitest'
import type { SorterResult } from 'antd/es/table/interface'
import type { DeliveryDetail } from '../../types/delivery'
import {
  sortDeliveryCustomerRows,
  updateDeliveryCustomerSortChain,
  type DeliveryCustomerTableRow,
} from './deliveryCustomerSorting'

describe('deliveryCustomerSorting', () => {
  it('keeps click order as primary-to-secondary priority', () => {
    const first = updateDeliveryCustomerSortChain([], sorter('customerPaperName', 'ascend'))
    const next = updateDeliveryCustomerSortChain(first, [
      sorter('customerPaperName', 'ascend'), sorter('customerDisplayWeight', 'descend'),
    ])

    expect(next).toEqual([
      { field: 'customerPaperName', direction: 'asc' },
      { field: 'customerDisplayWeight', direction: 'desc' },
    ])
  })

  it('sorts trace source rolls and keeps blank values last', () => {
    const rows = [row('a', 'A', 'M-10'), row('b', '', undefined), row('c', 'A', 'M-2')]

    const result = sortDeliveryCustomerRows(rows, [
      { field: 'customerPaperName', direction: 'asc' },
      { field: 'sourceMotherRoll', direction: 'asc' },
    ])

    expect(result.map(({ spec }) => spec.deliveryDetailUuid)).toEqual(['c', 'a', 'b'])
  })
})

function sorter(field: string, order: 'ascend' | 'descend') {
  return { field, columnKey: field, order } as SorterResult<DeliveryCustomerTableRow>
}

function row(uuid: string, paperName: string, source?: string): DeliveryCustomerTableRow {
  const detail: DeliveryDetail = {
    uuid, deliveryUuid: 'delivery', finishUuid: uuid, orderUuid: 'order',
    finishRollNo: uuid, paperName: 'paper', outWeight: 1, originalSummary: source,
  }
  return {
    detail,
    spec: {
      deliveryDetailUuid: uuid,
      detailVersion: 1,
      finishUuid: uuid,
      customerPaperName: paperName,
      calculationMode: 'KEEP',
      valueSource: 'PHYSICAL',
      specificationChanged: false,
      weightChanged: false,
      valid: true,
    },
  }
}
