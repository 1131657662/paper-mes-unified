import { describe, expect, it } from 'vitest'
import type { ProcessOrder } from '../../types/processOrder'
import type { BatchActions } from './ProcessOrderBatchToolbar'
import { buildMoreItems } from './processOrderBatchMenuItems'
import type { ProcessOrderListCapabilities } from './useProcessOrderListCapabilities'

const actions: BatchActions = {
  onBackRecord: () => undefined,
  onCalcFee: async () => undefined,
  onChangeStatus: () => undefined,
  onConfirmPrintAndToRecord: () => undefined,
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
  it('未完成人工确认时改为原子确认打印并转待回录', () => {
    const item = toRecordItem({ uuid: 'order-1', orderStatus: 2, printCount: 0, printStatus: 1 })

    expect(item).toMatchObject({ label: '确认打印并转待回录' })
    expect(item).not.toHaveProperty('disabled')
  })

  it('至少有一次打印确认记录时允许入口', () => {
    const item = toRecordItem({ uuid: 'order-1', orderStatus: 2, printCount: 1, printStatus: 1 })

    expect(item).toMatchObject({ label: '转待回录' })
    expect(item).not.toHaveProperty('disabled')
  })
})

function toRecordItem(record: ProcessOrder) {
  return buildMoreItems(record, actions, capabilities).find((item) => item?.key === 'to-record')
}
