import { describe, expect, it } from 'vitest'
import type { ProcessOrder } from '../../types/processOrder'
import type { BatchActions } from './ProcessOrderBatchToolbar'
import { buildMoreItems } from './processOrderBatchMenuItems'
import type { ProcessOrderListCapabilities } from './useProcessOrderListCapabilities'

const actions: BatchActions = {
  onBackRecord: () => undefined,
  onCalcFee: async () => undefined,
  onChangeStatus: () => undefined,
  onGoDelivery: () => undefined,
  onGoSettle: () => undefined,
  onManageRolls: () => undefined,
  onPrint: () => undefined,
  onSnapshotDiff: () => undefined,
  onVoidOrder: async () => undefined,
}
const capabilities: ProcessOrderListCapabilities = {
  canBackRecord: false,
  canCreateOrder: false,
  canManageDelivery: false,
  canManageOrder: true,
  canManageSettlement: false,
}

describe('加工单列表转待回录门禁', () => {
  it('打印次数为零时禁用入口，即使打印状态字段异常为已打印', () => {
    const item = toRecordItem({ uuid: 'order-1', orderStatus: 2, printCount: 0, printStatus: 1 })

    expect(item).toMatchObject({ disabled: true, label: '转待回录（请先确认打印）' })
  })

  it('至少有一次打印确认记录时允许入口', () => {
    const item = toRecordItem({ uuid: 'order-1', orderStatus: 2, printCount: 1 })

    expect(item).toMatchObject({ disabled: false, label: '转待回录' })
  })
})

function toRecordItem(record: ProcessOrder) {
  return buildMoreItems(record, actions, capabilities).find((item) => item?.key === 'to-record')
}
