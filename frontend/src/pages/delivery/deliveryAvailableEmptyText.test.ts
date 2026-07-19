import { describe, expect, it } from 'vitest'
import type { AvailableFinishPageVO } from '../../types/delivery'
import { deliveryAvailableEmptyText } from './deliveryAvailableEmptyText'

describe('deliveryAvailableEmptyText', () => {
  it('explains inventory excluded because warehouse is unassigned', () => {
    const message = deliveryAvailableEmptyText({
      customerUuid: 'customer-1',
      data: pageWithUnassigned(18),
      hasFilters: false,
      isError: false,
      scopeName: '成品',
      warehouseUuid: 'warehouse-1',
    })

    expect(message).toContain('18 卷成品尚未分配仓库')
  })

  it('asks for a warehouse before reporting empty inventory', () => {
    const message = deliveryAvailableEmptyText({
      customerUuid: 'customer-1',
      hasFilters: false,
      isError: false,
      scopeName: '成品',
    })

    expect(message).toBe('请先选择出库仓库')
  })
})

function pageWithUnassigned(count: number): AvailableFinishPageVO {
  return {
    records: [], total: 0, current: 1, size: 20, asOf: '2026-07-19 20:00:00',
    scopeCounts: { product: 0, remain: 0 },
    excluded: {
      unassignedWarehouseCount: count,
      otherWarehouseCount: 0,
      lockedCount: 0,
      invalidWeightCount: 0,
    },
  }
}
