import { Button, Space } from 'antd'
import type { ProcessOrder } from '../../types/processOrder'
import type { ProcessOrderListCapabilities } from './useProcessOrderListCapabilities'

interface Props {
  record: ProcessOrder
  capabilities: ProcessOrderListCapabilities
  onBackRecord: (uuid: string) => void
  onChangeStatus: (record: ProcessOrder, target: number, title: string) => void
  onEditDraft: (uuid: string) => void
  onGoDelivery: (record: ProcessOrder) => void
  onGoSettle: (record: ProcessOrder) => void
  onPrint: (record: ProcessOrder) => void
}

export default function ProcessOrderRowActions({ record, ...actions }: Props) {
  const available = rowActions(record, actions)
  if (available.length === 0) return null
  return (
    <Space size={0}>
      {available.map((action) => (
        <Button key={action.label} type="link" size="small" onClick={action.onClick}>{action.label}</Button>
      ))}
    </Space>
  )
}

function rowActions(record: ProcessOrder, actions: Omit<Props, 'record'>) {
  const status = record.orderStatus ?? 0
  if (status === 0 && actions.capabilities.canCreateOrder) return [{ label: '继续编辑', onClick: () => actions.onEditDraft(record.uuid) }]
  if (status === 1 && actions.capabilities.canManageOrder) return [{ label: '打印下发', onClick: () => actions.onPrint(record) }]
  if (status === 2 && actions.capabilities.canManageOrder) return [{ label: '转待回录', onClick: () => actions.onChangeStatus(record, 3, '确认车间已完成加工，转入待回录？') }]
  if (status === 3 && actions.capabilities.canBackRecord) return [{ label: '进入回录', onClick: () => actions.onBackRecord(record.uuid) }]
  if (status !== 4) return []
  const completedActions = []
  if (actions.capabilities.canManageDelivery) completedActions.push({ label: '创建出库', onClick: () => actions.onGoDelivery(record) })
  if (actions.capabilities.canManageSettlement) completedActions.push({ label: '生成结算', onClick: () => actions.onGoSettle(record) })
  return completedActions
}
