import { Button, Dropdown, Modal, Space } from 'antd'
import {
  CalculatorOutlined,
  DiffOutlined,
  FileDoneOutlined,
  InboxOutlined,
  NumberOutlined,
  PrinterOutlined,
  RollbackOutlined,
} from '@ant-design/icons'
import type { MenuProps } from 'antd'
import MesTooltip from '../../components/biz/MesTooltip'
import type { ProcessOrder } from '../../types/processOrder'

type MenuItems = NonNullable<MenuProps['items']>

export interface BatchActions {
  onBackRecord: (uuid: string) => void
  onCalcFee: (record: ProcessOrder) => Promise<void>
  onChangeStatus: (record: ProcessOrder, target: number) => Promise<void>
  onGoDelivery: () => void
  onGoSettle: () => void
  onManageRolls: (uuid: string) => void
  onPrint: (record: ProcessOrder) => void
  onSnapshotDiff: (uuid: string) => void
}

interface Props {
  selectedRows: ProcessOrder[]
  actions: BatchActions
}

export default function ProcessOrderBatchToolbar({ selectedRows, actions }: Props) {
  const record = selectedRows.length === 1 ? selectedRows[0] : undefined

  return (
    <div className={selectedRows.length > 0 ? 'process-order-batchbar is-active' : 'process-order-batchbar'}>
      <Space size={8}>
        <ActionButton icon={<PrinterOutlined />} disabled={!canPrint(record)} onClick={() => record && actions.onPrint(record)}>
          打印/补打
        </ActionButton>
        <ActionButton icon={<FileDoneOutlined />} disabled={record?.orderStatus !== 3} onClick={() => record && actions.onBackRecord(record.uuid)}>
          进入回录
        </ActionButton>
        <ActionButton icon={<NumberOutlined />} disabled={!canManageRolls(record)} onClick={() => record && actions.onManageRolls(record.uuid)}>
          成品号
        </ActionButton>
        <ActionButton icon={<CalculatorOutlined />} disabled={!canCalcFee(record)} onClick={() => record && confirmFee(record, actions)}>
          重算计费
        </ActionButton>
        <Dropdown menu={{ items: buildMoreItems(record, actions) }} trigger={['click']} disabled={!record}>
          <Button>更多处理</Button>
        </Dropdown>
      </Space>
    </div>
  )
}

function ActionButton({
  children,
  disabled,
  icon,
  onClick,
}: {
  children: React.ReactNode
  disabled: boolean
  icon: React.ReactNode
  onClick: () => void
}) {
  return (
    <MesTooltip title={disabled ? '请选择一张符合状态的加工单' : undefined}>
      <Button icon={icon} disabled={disabled} onClick={onClick}>
        {children}
      </Button>
    </MesTooltip>
  )
}

function buildMoreItems(record: ProcessOrder | undefined, actions: BatchActions): MenuItems {
  if (!record) return []
  const status = record.orderStatus ?? 0
  const items: MenuItems = []
  if (status === 2) items.push(item('to-record', '转待回录', () => confirmStatus(record, 3, '确认车间已完成加工，转入待回录？', actions)))
  if (status >= 4) items.push(item('snapshot', '查看快照差异', () => actions.onSnapshotDiff(record.uuid), <DiffOutlined />))
  if (status === 4) items.push(item('delivery', '创建出库', actions.onGoDelivery, <InboxOutlined />))
  if (status === 4) items.push(item('settle', '生成结算', actions.onGoSettle))
  if (status === 3) items.push(item('rollback-pending', '回退待下发', () => confirmStatus(record, 1, '确认回退到待下发？会清理完成快照和回录信息。', actions), <RollbackOutlined />, true))
  if (status === 4) items.push(item('rollback-record', '回退待回录', () => confirmStatus(record, 3, '确认回退到待回录？', actions), <RollbackOutlined />, true))
  return items
}

function item(key: string, label: string, onClick: () => void, icon?: React.ReactNode, danger = false): MenuItems[number] {
  return { key, label, icon, danger, onClick }
}

function canPrint(record?: ProcessOrder) {
  return record != null && (record.orderStatus ?? 0) >= 1
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
  Modal.confirm({
    title,
    content: record.orderNo,
    onOk: () => actions.onChangeStatus(record, targetStatus),
  })
}
