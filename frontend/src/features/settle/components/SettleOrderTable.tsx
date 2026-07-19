import { Button, Progress, Space, Tag, Typography } from 'antd'
import dayjs from 'dayjs'
import { ProTable } from '@ant-design/pro-components'
import type { ProColumns } from '@ant-design/pro-components'
import type { ReactNode } from 'react'
import type { TableRowSelection } from 'antd/es/table/interface'
import TooltipText from '../../../components/biz/TooltipText'
import { mesProTableOptions } from '../../../components/biz/mesProTableOptions'
import { renderTableToolbarPortal } from '../../../components/biz/tableToolbarPortalUtils'
import { useResizableTableColumns } from '../../../components/useResizableTableColumns'
import { useTableColumnsState } from '../../../hooks/useTableColumnsState'
import { SETTLE_STATUS, SETTLE_TYPE } from '../../../constants/settle'
import type { SettleOrder } from '../../../types/settle'
import { formatMoney, formatPercent } from '../utils/settleFormatters'

interface Props {
  canReceiveSettle?: boolean
  collectionMode?: boolean
  data: SettleOrder[]
  fixedHeader?: boolean
  loading: boolean
  onReload?: () => void
  rowClassName?: (record: SettleOrder) => string
  rowSelection?: TableRowSelection<SettleOrder>
  onDetail: (record: SettleOrder) => void
  onRow?: (record: SettleOrder) => React.HTMLAttributes<HTMLElement>
  onReceive: (record: SettleOrder) => void
  onRemind?: (record: SettleOrder) => void
}

export default function SettleOrderTable({
  canReceiveSettle = false,
  collectionMode = false,
  data,
  fixedHeader = false,
  loading,
  onDetail,
  onReload,
  onRow,
  onReceive,
  onRemind,
  rowClassName,
  rowSelection,
}: Props) {
  const columns = buildColumns({ canReceiveSettle, collectionMode, onDetail, onReceive, onRemind })
  const columnsState = useTableColumnsState('table-columns-settle-orders')
  const resizable = useResizableTableColumns<SettleOrder, ProColumns<SettleOrder>>(columns, 'settle-orders')

  return (
    <ProTable<SettleOrder>
      className="settle-order-table mes-table-card"
      rowKey="uuid"
      loading={loading}
      columns={resizable.columns}
      columnsState={columnsState}
      components={resizable.components}
      dataSource={data}
      pagination={false}
      rowClassName={rowClassName}
      rowSelection={rowSelection}
      onRow={onRow}
      bordered
      locale={{ emptyText: '暂无结算单' }}
      cardProps={false}
      headerTitle={false}
      options={mesProTableOptions(onReload)}
      optionsRender={renderTableToolbarPortal}
      search={false}
      scroll={{ x: resizable.scrollX, ...(fixedHeader ? { y: '100%' } : {}) }}
      tableAlertRender={false}
      tableAlertOptionRender={false}
      tableLayout="fixed"
      toolBarRender={() => []}
    />
  )
}

function buildColumns(actions: {
  canReceiveSettle: boolean
  collectionMode: boolean
  onDetail: (record: SettleOrder) => void
  onReceive: (record: SettleOrder) => void
  onRemind?: (record: SettleOrder) => void
}): ProColumns<SettleOrder>[] {
  return [
    {
      title: '结算单',
      dataIndex: 'settleNo',
      fixed: 'left',
      width: 190,
      minWidth: 180,
      render: (value, record) => (
        <div className="settle-cell-stack mes-cell-stack">
          <Typography.Text strong>{value}</Typography.Text>
          <span>{SETTLE_TYPE[record.settleType] ?? '-'}</span>
        </div>
      ),
    },
    { title: '客户', dataIndex: 'customerName', width: 180, minWidth: 160, render: (_, record) => textCell(record.customerName) },
    { title: '结算日期', dataIndex: 'settleDate', width: 124, hideInTable: actions.collectionMode },
    {
      title: '到期 / 跟进',
      dataIndex: 'dueDate',
      width: 164,
      render: (_, record) => <CollectionDueCell record={record} />,
    },
    {
      title: actions.collectionMode ? '未收' : '应收',
      dataIndex: 'totalAmount',
      align: 'right',
      width: 118,
      render: (_, record) => formatMoney(actions.collectionMode ? record.unreceivedAmount : record.totalAmount),
    },
    {
      title: '收款进度',
      dataIndex: 'receivedAmount',
      width: 268,
      minWidth: 248,
      hideInTable: actions.collectionMode,
      render: (_, record) => <ReceiveProgress record={record} />,
    },
    {
      title: '状态',
      dataIndex: 'settleStatus',
      width: 112,
      render: (_, record) => {
        const status = SETTLE_STATUS[record.settleStatus]
        if (!status) return '-'
        return (
          <Space size={4} wrap>
            <Tag className="mes-status-tag" color={status.color}>{status.text}</Tag>
            {isOverdue(record) && <Tag color="error">已逾期</Tag>}
          </Space>
        )
      },
    },
    {
      title: '操作',
      key: 'actions',
      className: 'settle-order-table__actions-cell',
      fixed: 'right',
      width: actions.collectionMode ? 190 : 210,
      minWidth: actions.collectionMode ? 190 : 210,
      render: (_, record) => (
        <Space className="mes-action-buttons">
          <Button type="link" size="small" onClick={() => actions.onDetail(record)}>详情</Button>
          {actions.canReceiveSettle && [1, 2].includes(record.settleStatus) && (
            <Button type="link" size="small" onClick={() => actions.onReceive(record)}>收款</Button>
          )}
          {actions.canReceiveSettle && actions.onRemind && [1, 2].includes(record.settleStatus) && (
            <Button type="link" size="small" onClick={() => actions.onRemind?.(record)}>提醒</Button>
          )}
        </Space>
      ),
    },
  ]
}

function CollectionDueCell({ record }: { record: SettleOrder }) {
  const due = dueText(record.dueDate)
  return (
    <div className="settle-cell-stack mes-cell-stack">
      <Typography.Text type={due.tone}>{due.text}</Typography.Text>
      <span>{record.reminderCount ? `已提醒 ${record.reminderCount} 次 · ${record.lastReminderBy || '-'}` : '尚未提醒'}</span>
    </div>
  )
}

function dueText(value?: string): { text: string; tone?: 'danger' | 'warning' | 'secondary' } {
  if (!value) return { text: '未设置到期日', tone: 'secondary' }
  const days = dayjs(value).startOf('day').diff(dayjs().startOf('day'), 'day')
  if (days < 0) return { text: `${value} · 逾期 ${Math.abs(days)} 天`, tone: 'danger' }
  if (days === 0) return { text: `${value} · 今日到期`, tone: 'warning' }
  return { text: `${value} · ${days} 天后` }
}

function ReceiveProgress({ record }: { record: SettleOrder }) {
  const percent = Number(formatPercent(record.receivedAmount ?? 0, record.totalAmount ?? 0).replace('%', ''))
  return (
    <div className="settle-cell-stack mes-cell-stack">
      <Progress percent={percent} size="small" />
      <span>已结清 {formatMoney(record.receivedAmount)} / 未收 {formatMoney(record.unreceivedAmount)}</span>
      <span>现金 {formatMoney(record.cashReceivedAmount)} / 废纸 {formatMoney(record.scrapOffsetAmount)} / 优惠 {formatMoney(record.discountAmount)}</span>
    </div>
  )
}

function textCell(value?: ReactNode) {
  return <TooltipText value={value} />
}

function isOverdue(record: SettleOrder) {
  return [1, 2].includes(record.settleStatus)
    && Number(record.unreceivedAmount ?? 0) > 0
    && Boolean(record.dueDate)
    && dayjs(record.dueDate).isBefore(dayjs(), 'day')
}
