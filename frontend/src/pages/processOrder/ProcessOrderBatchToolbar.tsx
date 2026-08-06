import { Button, Dropdown, Modal, Space } from 'antd'
import {
  CalculatorOutlined,
  FileDoneOutlined,
  NumberOutlined,
  PrinterOutlined,
} from '@ant-design/icons'
import type { ProcessOrder } from '../../types/processOrder'
import { hasConfirmedProcessOrderPrint, hasHistoricalUnconfirmedPrint } from '../../features/processOrderDetail/processOrderPrintStage'
import type { ProcessOrderListCapabilities } from './useProcessOrderListCapabilities'
import { buildMoreItems } from './processOrderBatchMenuItems'

export interface BatchActions {
  onBackRecord: (uuid: string) => void
  onCalcFee: (record: ProcessOrder) => Promise<void>
  onChangeStatus: (record: ProcessOrder, target: number, title: string) => void
  onConfirmPrintAndToRecord: (record: ProcessOrder) => void
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
        {capabilities.canManageOrder && canPrint(record) && <Button icon={<PrinterOutlined />} onClick={() => actions.onPrint(record)}>{printActionLabel(record)}</Button>}
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

function canPrint(record?: ProcessOrder) {
  const status = record?.orderStatus ?? 0
  return record != null && status >= 1 && status !== 6
}

function printActionLabel(record: ProcessOrder): string {
  const status = record.orderStatus ?? 0
  const hasPrinted = hasConfirmedProcessOrderPrint(record.printStatus, record.printCount)
  return hasHistoricalUnconfirmedPrint(status, hasPrinted) ? '补确认历史打印' : '打印/补打'
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
