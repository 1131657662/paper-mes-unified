import { Tag } from 'antd'
import type { ReactNode } from 'react'
import type { ProColumns } from '@ant-design/pro-components'
import type { ProcessOrder } from '../../types/processOrder'
import TooltipText from '../../components/biz/TooltipText'
import { ORDER_STATUS, PRIORITY } from '../../constants/processOrder'
import ProcessOrderRowActions from './ProcessOrderRowActions'
import { BillingCell, OrderNoCell, OrderScheduleCell, PriorityPill, ProductionSummary } from './ProcessOrderListCells'

interface ColumnOptions {
  customerEnum: Record<string, { text: string }>
  onBackRecord: (uuid: string) => void
  onCalcFee: (record: ProcessOrder) => Promise<void>
  onChangeStatus: (record: ProcessOrder, target: number, title: string) => void
  onDetail: (uuid: string) => void
  onEditDraft: (uuid: string) => void
  onGoDelivery: () => void
  onGoSettle: () => void
  onManageRolls: (uuid: string) => void
  onPrint: (record: ProcessOrder) => void
  onSnapshotDiff: (uuid: string) => void
  onVoidOrder: (record: ProcessOrder) => Promise<void>
}

const statusValueEnum = Object.fromEntries(
  Object.entries(ORDER_STATUS).map(([k, v]) => [k, { text: v.text }]),
)

const priorityValueEnum = Object.fromEntries(
  Object.entries(PRIORITY).map(([k, v]) => [k, { text: v }]),
)

export function buildProcessOrderColumns(options: ColumnOptions): ProColumns<ProcessOrder>[] {
  return [
    { title: '加工单', dataIndex: 'orderNo', width: 170, hideInSearch: true, render: (_, record) => <OrderNoCell record={record} /> },
    { title: '加工单号/客户', dataIndex: 'keyword', hideInTable: true },
    { title: '客户', dataIndex: 'customerName', width: 150, hideInSearch: true, render: textCell },
    { title: '客户', dataIndex: 'customerUuid', hideInTable: true, valueType: 'select', valueEnum: options.customerEnum },
    { title: '制单日期', dataIndex: 'orderDate', width: 112, hideInSearch: true },
    { title: '制单日期', dataIndex: 'dateRange', valueType: 'dateRange', hideInTable: true },
    { title: '优先级', dataIndex: 'priority', width: 86, valueType: 'select', valueEnum: priorityValueEnum, hideInSearch: true, render: (_, record) => <PriorityPill value={record.priority} /> },
    { title: '安排', dataIndex: 'schedule', width: 150, hideInSearch: true, render: (_, record) => <OrderScheduleCell record={record} /> },
    { title: '结算/开票', dataIndex: 'billing', width: 122, hideInSearch: true, render: (_, record) => <BillingCell record={record} /> },
    { title: '状态', dataIndex: 'orderStatus', width: 98, valueType: 'select', valueEnum: statusValueEnum, render: (_, record) => renderStatus(record) },
    { title: '下发', dataIndex: 'printStatus', width: 98, hideInSearch: true, render: (_, record) => renderPrintStatus(record) },
    { title: '生产统计', dataIndex: 'productionSummary', width: 252, hideInSearch: true, render: (_, record) => <ProductionSummary record={record} /> },
    { title: '费用', dataIndex: 'totalAmount', width: 116, hideInSearch: true, render: (_, record) => renderMoney(record.totalAmount) },
    { title: '操作', key: 'actions', valueType: 'option', width: 136, fixed: 'right', render: (_, record) => <ProcessOrderRowActions record={record} {...options} /> },
  ]
}

function textCell(value?: ReactNode) {
  return <TooltipText value={value} />
}

function renderStatus(record: ProcessOrder) {
  if (record.orderStatus == null) return '-'
  const status = ORDER_STATUS[record.orderStatus]
  return <Tag color={status?.color}>{status?.text ?? '-'}</Tag>
}

function renderPrintStatus(record: ProcessOrder) {
  if (record.printStatus !== 1) return <Tag>未下发</Tag>
  return <Tag color="processing">已下发 {record.printCount ?? 1} 次</Tag>
}

function renderMoney(value?: number) {
  if (value == null || value === 0) return '-'
  return `¥${formatNumber(value, 2)}`
}

function formatNumber(value: number, digits: number) {
  return value.toLocaleString('zh-CN', {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  })
}
