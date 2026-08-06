import { describe, expect, it } from 'vitest'
import type { SorterResult } from 'antd/es/table/interface'
import type { DeliveryInventoryFinish, DeliveryInventoryOrderGroup } from '../../types/deliveryInventory'
import {
  sortDeliveryInventoryFinishes,
  sortDeliveryInventoryOrderGroups,
  updateDeliveryInventoryFinishSortChain,
} from './deliveryInventorySorting'

describe('deliveryInventorySorting', () => {
  it('按点击顺序追加多列排序并支持取消', () => {
    const first = updateDeliveryInventoryFinishSortChain([], sorter('paperName', 'ascend'))
    const second = updateDeliveryInventoryFinishSortChain(first, [sorter('paperName', 'ascend'), sorter('remainingWeight', 'descend')])
    const cancelled = updateDeliveryInventoryFinishSortChain(second, [sorter('paperName', undefined), sorter('remainingWeight', 'descend')])

    expect(second).toEqual([
      { field: 'paperName', direction: 'asc' },
      { field: 'remainingWeight', direction: 'desc' },
    ])
    expect(cancelled).toEqual([{ field: 'remainingWeight', direction: 'desc' }])
  })

  it('空值置底且同值保持原始顺序', () => {
    const rows = [finish('A-2', '纸', undefined), finish('A-1', '纸', 'CK-100'), finish('A-3', '纸', 'CK-100')]
    const sorted = sortDeliveryInventoryFinishes(rows, [{ field: 'deliveryNo', direction: 'asc' }])

    expect(sorted.map((row) => row.finishRollNo)).toEqual(['A-1', 'A-3', 'A-2'])
  })

  it('加工单分组按数值字段排序', () => {
    const rows = [group('JG-2', 10), group('JG-1', 2)]
    const sorted = sortDeliveryInventoryOrderGroups(rows, [{ field: 'totalRollCount', direction: 'asc' }])

    expect(sorted.map((row) => row.orderNo)).toEqual(['JG-1', 'JG-2'])
  })
})

function sorter(field: keyof DeliveryInventoryFinish, order: 'ascend' | 'descend' | undefined): SorterResult<DeliveryInventoryFinish> {
  return { field, columnKey: field, order }
}

function finish(finishRollNo: string, paperName: string, deliveryNo: string | undefined): DeliveryInventoryFinish {
  return {
    customerUuid: 'customer', customerName: '客户', finishUuid: finishRollNo, finishRollNo,
    orderUuid: 'order', orderNo: 'JG-1', paperName, remainingWeight: 100, deliveryNo, stockState: 1,
  }
}

function group(orderNo: string, totalRollCount: number): DeliveryInventoryOrderGroup {
  return { orderUuid: orderNo, orderNo, totalRollCount, totalWeight: 0, availableRollCount: 0, lockedRollCount: 0, finishes: [] }
}
