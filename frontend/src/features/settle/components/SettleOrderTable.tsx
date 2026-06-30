import { Button, Progress, Space, Tag, Typography } from 'antd'
import { ProTable } from '@ant-design/pro-components'
import type { ProColumns } from '@ant-design/pro-components'
import type { ReactNode } from 'react'
import type { TableRowSelection } from 'antd/es/table/interface'
import TooltipText from '../../../components/biz/TooltipText'
import { renderTableToolbarPortal } from '../../../components/biz/TableToolbarPortal'
import { useTableColumnsState } from '../../../hooks/useTableColumnsState'
import { useResizableTableColumns } from '../../../components/useResizableTableColumns'
import { SETTLE_STATUS, SETTLE_TYPE } from '../../../constants/settle'
import type { SettleOrder } from '../../../types/settle'
import { formatMoney, formatPercent } from '../utils/settleFormatters'

interface Props {
  data: SettleOrder[]
  loading: boolean
  onReload?: () => void
  rowClassName?: (record: SettleOrder) => string
  rowSelection?: TableRowSelection<SettleOrder>
  onDetail: (record: SettleOrder) => void
  onRow?: (record: SettleOrder) => React.HTMLAttributes<HTMLElement>
  onReceive: (record: SettleOrder) => void
}

export default function SettleOrderTable({
  data,
  loading,
  onDetail,
  onReload,
  onRow,
  onReceive,
  rowClassName,
  rowSelection,
}: Props) {
  const columns = buildColumns({ onDetail, onReceive })
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
      cardProps={false}
      headerTitle={false}
      options={{ density: true, reload: onReload ? () => onReload() : true, setting: true }}
      optionsRender={renderTableToolbarPortal}
      search={false}
      scroll={{ x: resizable.scrollX, y: '100%' }}
      tableAlertRender={false}
      tableAlertOptionRender={false}
      tableLayout="fixed"
      toolBarRender={() => []}
    />
  )
}

function buildColumns(actions: {
  onDetail: (record: SettleOrder) => void
  onReceive: (record: SettleOrder) => void
}): ProColumns<SettleOrder>[] {
  return [
    {
      title: '结算单',
      dataIndex: 'settleNo',
      fixed: 'left',
      width: 155,
      render: (value, record) => (
        <div className="settle-cell-stack mes-cell-stack">
          <Typography.Text strong>{value}</Typography.Text>
          <span>{SETTLE_TYPE[record.settleType] ?? '-'}</span>
        </div>
      ),
    },
    { title: '客户', dataIndex: 'customerName', width: 140, render: (_, record) => textCell(record.customerName) },
    { title: '结算日期', dataIndex: 'settleDate', width: 110 },
    { title: '应收', dataIndex: 'totalAmount', align: 'right', width: 110, render: (_, record) => formatMoney(record.totalAmount) },
    {
      title: '收款进度',
      dataIndex: 'receivedAmount',
      width: 160,
      render: (_, record) => (
        <div className="settle-cell-stack mes-cell-stack">
          <Progress
            percent={Number(formatPercent(record.receivedAmount ?? 0, record.totalAmount ?? 0).replace('%', ''))}
            size="small"
          />
          <span>{formatMoney(record.receivedAmount)} / {formatMoney(record.unreceivedAmount)}未收</span>
        </div>
      ),
    },
    {
      title: '状态',
      dataIndex: 'settleStatus',
      width: 105,
      render: (_, record) => {
        const status = SETTLE_STATUS[record.settleStatus]
        return status ? <Tag className="mes-status-tag" color={status.color}>{status.text}</Tag> : '-'
      },
    },
    {
      title: '操作',
      key: 'actions',
      className: 'settle-order-table__actions-cell',
      fixed: 'right',
      width: 150,
      render: (_, record) => (
        <Space className="mes-action-buttons">
          <Button type="link" size="small" onClick={() => actions.onDetail(record)}>
            详情
          </Button>
          {record.settleStatus !== 3 && (
            <Button type="link" size="small" onClick={() => actions.onReceive(record)}>
              收款
            </Button>
          )}
        </Space>
      ),
    },
  ]
}

function textCell(value?: ReactNode) {
  return <TooltipText value={value} />
}
