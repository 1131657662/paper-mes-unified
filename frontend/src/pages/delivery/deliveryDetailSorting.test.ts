import type { SorterResult } from 'antd/es/table/interface'
import { describe, expect, it } from 'vitest'
import type { DeliveryDetail } from '../../types/delivery'
import {
  sortDeliveryDetails,
  updateDeliverySortChain,
  type DeliverySortSpec,
} from './deliveryDetailSorting'

describe('deliveryDetailSorting', () => {
  it('keeps click order as primary-to-secondary priority', () => {
    const first = updateDeliverySortChain([], sorter('paperName', 'ascend'))
    const next = updateDeliverySortChain(first, [sorter('paperName', 'ascend'), sorter('gramWeight', 'ascend')])

    expect(next).toEqual([
      { field: 'paperName', direction: 'asc' },
      { field: 'gramWeight', direction: 'asc' },
    ])
  })

  it('removes only the cancelled column from a multi-sort chain', () => {
    const current: DeliverySortSpec[] = [
      { field: 'paperName', direction: 'asc' },
      { field: 'gramWeight', direction: 'desc' },
      { field: 'outWeight', direction: 'asc' },
    ]
    const next = updateDeliverySortChain(current, [sorter('paperName', 'ascend'), sorter('outWeight', 'ascend')])

    expect(next).toEqual([
      { field: 'paperName', direction: 'asc' },
      { field: 'outWeight', direction: 'asc' },
    ])
  })

  it('sorts entire rows stably and puts empty values last', () => {
    const rows = [row('a', 'A', 10), row('b', '', 20), row('c', 'A', 5)]
    const result = sortDeliveryDetails(rows, [
      { field: 'paperName', direction: 'asc' },
      { field: 'gramWeight', direction: 'desc' },
    ])

    expect(result.map((item) => item.uuid)).toEqual(['a', 'c', 'b'])
  })
})

function sorter(field: string, order: 'ascend' | 'descend'): SorterResult<DeliveryDetail> {
  return { field, columnKey: field, order } as SorterResult<DeliveryDetail>
}

function row(uuid: string, paperName: string, gramWeight: number): DeliveryDetail {
  return {
    uuid, deliveryUuid: 'delivery', finishUuid: uuid, orderUuid: 'order', finishRollNo: uuid,
    paperName, gramWeight, outWeight: gramWeight,
  }
}
