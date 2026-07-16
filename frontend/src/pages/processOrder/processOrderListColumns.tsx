import type { ReactNode } from 'react'
import type { ProColumns } from '@ant-design/pro-components'
import type { ProcessOrder } from '../../types/processOrder'
import TooltipText from '../../components/biz/TooltipText'
import { ORDER_STATUS } from '../../constants/processOrder'
import ProcessOrderRowActions from './ProcessOrderRowActions'
import { BillingCell, OrderDateScheduleCell, OrderNoCell, OrderStatusCell, ProductionSummary } from './ProcessOrderListCells'

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

export function buildProcessOrderColumns(options: ColumnOptions): ProColumns<ProcessOrder>[] {
  return [
    { title: '加工单', dataIndex: 'orderNo', width: 180, minWidth: 170, hideInSearch: true, render: (_, record) => <OrderNoCell onDetail={options.onDetail} record={record} /> },
    { title: '加工单号/客户', dataIndex: 'keyword', hideInTable: true },
    { title: '客户', dataIndex: 'customerName', width: 125, minWidth: 120, hideInSearch: true, render: textCell },
    { title: '客户', dataIndex: 'customerUuid', hideInTable: true, valueType: 'select', valueEnum: options.customerEnum },
    { title: '日期/安排', dataIndex: 'schedule', width: 125, minWidth: 120, hideInSearch: true, render: (_, record) => <OrderDateScheduleCell record={record} /> },
    { title: '结算/费用', dataIndex: 'billing', width: 150, minWidth: 145, hideInSearch: true, render: (_, record) => <BillingCell record={record} /> },
    { title: '状态/下发', dataIndex: 'orderStatus', width: 110, minWidth: 105, valueType: 'select', valueEnum: statusValueEnum, render: (_, record) => <OrderStatusCell record={record} /> },
    { title: '生产统计', dataIndex: 'productionSummary', width: 200, minWidth: 190, hideInSearch: true, render: (_, record) => <ProductionSummary record={record} /> },
    { title: '操作', key: 'actions', valueType: 'option', width: 85, minWidth: 85, fixed: 'right', render: (_, record) => <ProcessOrderRowActions record={record} {...options} /> },
  ]
}

function textCell(value?: ReactNode) {
  return <TooltipText value={value} />
}
