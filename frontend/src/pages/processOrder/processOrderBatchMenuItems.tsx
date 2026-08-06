import type { ReactNode } from 'react'
import type { MenuProps } from 'antd'
import { DiffOutlined, InboxOutlined, RollbackOutlined, StopOutlined } from '@ant-design/icons'
import type { ProcessOrder } from '../../types/processOrder'
import { hasConfirmedProcessOrderPrint, hasHistoricalUnconfirmedPrint } from '../../features/processOrderDetail/processOrderPrintStage'
import type { BatchActions } from './ProcessOrderBatchToolbar'
import type { ProcessOrderListCapabilities } from './useProcessOrderListCapabilities'

type MenuItems = NonNullable<MenuProps['items']>

export function buildMoreItems(
  record: ProcessOrder | undefined,
  actions: BatchActions,
  capabilities: ProcessOrderListCapabilities,
): MenuItems {
  if (!record) return []
  const status = record.orderStatus ?? 0
  const hasPrinted = hasConfirmedProcessOrderPrint(record.printStatus, record.printCount)
  const historicalPrintRisk = hasHistoricalUnconfirmedPrint(status, hasPrinted)
  const items: MenuItems = []
  if (capabilities.canManageOrder && status === 2) addCompletionItem(items, record, actions)
  if (status === 4 || status === 5) items.push(item('snapshot', '查看快照差异', () => actions.onSnapshotDiff(record.uuid), <DiffOutlined />))
  if (!historicalPrintRisk && capabilities.canManageDelivery && (status === 4 || status === 5)) items.push(item('delivery', '创建出库', () => actions.onGoDelivery(record), <InboxOutlined />))
  if (!historicalPrintRisk && capabilities.canManageSettlement && status === 4) items.push(item('settle', '生成结算', () => actions.onGoSettle(record)))
  if (capabilities.canManageOrder) addManageItems(items, record, actions)
  return items
}

function addCompletionItem(items: MenuItems, record: ProcessOrder, actions: BatchActions) {
  items.push({
    key: 'to-record',
    label: '确认打印并转待回录',
    onClick: () => actions.onConfirmPrintAndToRecord(record),
  })
}

function addManageItems(items: MenuItems, record: ProcessOrder, actions: BatchActions) {
  const status = record.orderStatus ?? 0
  if (status === 1) items.push(item('rollback-draft', '回退草稿编辑', () => actions.onChangeStatus(record, 0, '确认回退到草稿继续编辑？已生成的工序、成品号和打印快照会失效。'), <RollbackOutlined />, true))
  if (status === 2) items.push(item('rollback-pending-from-processing', '回退待下发', () => actions.onChangeStatus(record, 1, '确认回退到待下发？已打印快照会失效，需要重新打印下发。'), <RollbackOutlined />, true))
  if (status === 3) items.push(item('rollback-pending', '回退待下发', () => actions.onChangeStatus(record, 1, '确认回退到待下发？会清理完成快照和回录信息。'), <RollbackOutlined />, true))
  if (status === 3 || status === 4) items.push(item('rollback-draft', '回退草稿编辑', () => actions.onChangeStatus(record, 0, '确认回退到草稿更换母卷？会清理下发、回录、成品号和工序产物数据。'), <RollbackOutlined />, true))
  if (status === 4) items.push(item('rollback-record', '回退待回录', () => actions.onChangeStatus(record, 3, '确认回退到待回录？'), <RollbackOutlined />, true))
  if (status >= 0 && status <= 2) items.push(item('void-order', '作废加工单', () => actions.onVoidOrder(record), <StopOutlined />, true))
}

function item(key: string, label: string, onClick: () => void, icon?: ReactNode, danger = false): MenuItems[number] {
  return { key, label, icon, danger, onClick }
}
