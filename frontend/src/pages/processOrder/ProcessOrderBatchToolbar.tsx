import { Button, Dropdown, Modal, Space } from 'antd'
import {
  CalculatorOutlined,
  DiffOutlined,
  FileDoneOutlined,
  InboxOutlined,
  NumberOutlined,
  PrinterOutlined,
  RollbackOutlined,
  StopOutlined,
} from '@ant-design/icons'
import type { MenuProps } from 'antd'
import type { ProcessOrder } from '../../types/processOrder'
import type { ProcessOrderListCapabilities } from './useProcessOrderListCapabilities'

type MenuItems = NonNullable<MenuProps['items']>

export interface BatchActions {
  onBackRecord: (uuid: string) => void
  onCalcFee: (record: ProcessOrder) => Promise<void>
  onChangeStatus: (record: ProcessOrder, target: number, title: string) => void
  onGoDelivery: (record: ProcessOrder) => void
  onGoSettle: (record: ProcessOrder) => void
  onManageRolls: (uuid: string) => void
  onPrint: (record: ProcessOrder) => void
  onSnapshotDiff: (uuid: string) => void
  onVoidOrder: (record: ProcessOrder) => Promise<void>
}

interface Props {
  selectedRows: ProcessOrder[]
  actions: BatchActions
  capabilities: ProcessOrderListCapabilities
}

export default function ProcessOrderBatchToolbar({ selectedRows, actions, capabilities }: Props) {
  const record = selectedRows.length === 1 ? selectedRows[0] : undefined
  if (!record) return null
  const moreItems = buildMoreItems(record, actions, capabilities)

  return (
    <div className="process-order-batchbar is-active">
      <span className="process-order-batchbar__selection">已选 <strong>{record.orderNo}</strong></span>
      <Space size={8}>
        {capabilities.canManageOrder && canPrint(record) && <Button icon={<PrinterOutlined />} onClick={() => actions.onPrint(record)}>打印/补打</Button>}
        {capabilities.canBackRecord && record.orderStatus === 3 && <Button icon={<FileDoneOutlined />} onClick={() => actions.onBackRecord(record.uuid)}>进入回录</Button>}
        {capabilities.canManageOrder && canManageRolls(record) && <Button icon={<NumberOutlined />} onClick={() => actions.onManageRolls(record.uuid)}>成品号</Button>}
        {capabilities.canManageOrder && canCalcFee(record) && <Button icon={<CalculatorOutlined />} onClick={() => confirmFee(record, actions)}>重算计费</Button>}
        {moreItems.length > 0 && (
          <Dropdown menu={{ items: moreItems }} trigger={['click']}>
            <Button>更多处理</Button>
          </Dropdown>
        )}
      </Space>
    </div>
  )
}

function buildMoreItems(record: ProcessOrder | undefined, actions: BatchActions, capabilities: ProcessOrderListCapabilities): MenuItems {
  if (!record) return []
  const status = record.orderStatus ?? 0
  const items: MenuItems = []
  if (capabilities.canManageOrder && status === 2) items.push(item('to-record', '转待回录', () => confirmStatus(record, 3, '确认车间已完成加工，转入待回录？', actions)))
  if (status === 4 || status === 5) items.push(item('snapshot', '查看快照差异', () => actions.onSnapshotDiff(record.uuid), <DiffOutlined />))
  if (capabilities.canManageDelivery && status === 4) items.push(item('delivery', '创建出库', () => actions.onGoDelivery(record), <InboxOutlined />))
  if (capabilities.canManageSettlement && status === 4) items.push(item('settle', '生成结算', () => actions.onGoSettle(record)))
  if (capabilities.canManageOrder) addManageItems(items, record, actions)
  return items
}

function addManageItems(items: MenuItems, record: ProcessOrder, actions: BatchActions) {
  const status = record.orderStatus ?? 0
  if (status === 1) items.push(item('rollback-draft', '回退草稿编辑', () => confirmStatus(record, 0, '确认回退到草稿继续编辑？已生成的工序、成品号和打印快照会失效。', actions), <RollbackOutlined />, true))
  if (status === 2) items.push(item('rollback-pending-from-processing', '回退待下发', () => confirmStatus(record, 1, '确认回退到待下发？已打印快照会失效，需要重新打印下发。', actions), <RollbackOutlined />, true))
  if (status === 3) items.push(item('rollback-pending', '回退待下发', () => confirmStatus(record, 1, '确认回退到待下发？会清理完成快照和回录信息。', actions), <RollbackOutlined />, true))
  if (status === 3 || status === 4) items.push(item('rollback-draft', '回退草稿编辑', () => confirmStatus(record, 0, '确认回退到草稿更换母卷？会清理下发、回录、成品号和工序产物数据。', actions), <RollbackOutlined />, true))
  if (status === 4) items.push(item('rollback-record', '回退待回录', () => confirmStatus(record, 3, '确认回退到待回录？', actions), <RollbackOutlined />, true))
  if (status >= 0 && status <= 2) items.push(item('void-order', '作废加工单', () => actions.onVoidOrder(record), <StopOutlined />, true))
}

function item(key: string, label: string, onClick: () => void, icon?: React.ReactNode, danger = false): MenuItems[number] {
  return { key, label, icon, danger, onClick }
}

function canPrint(record?: ProcessOrder) {
  const status = record?.orderStatus ?? 0
  return record != null && status >= 1 && status !== 6
}

function canManageRolls(record?: ProcessOrder) {
  const status = record?.orderStatus ?? 0
  return status >= 1 && status <= 3
}

function canCalcFee(record?: ProcessOrder) {
  const status = record?.orderStatus ?? 0
  return status >= 2 && status <= 4
}

function confirmFee(record: ProcessOrder, actions: BatchActions) {
  Modal.confirm({
    title: '确认重算计费？',
    content: record.orderNo,
    onOk: () => actions.onCalcFee(record),
  })
}

function confirmStatus(record: ProcessOrder, targetStatus: number, title: string, actions: BatchActions) {
  actions.onChangeStatus(record, targetStatus, title)
}
