import { Button, Progress, Space, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { SETTLE_STATUS, SETTLE_TYPE } from '../../../constants/settle'
import type { SettleOrder } from '../../../types/settle'
import { formatMoney, formatPercent } from '../utils/settleFormatters'

interface Props {
  data: SettleOrder[]
  loading: boolean
  page: number
  pageSize: number
  total: number
  onDetail: (record: SettleOrder) => void
  onPageChange: (page: number, pageSize: number) => void
  onReceive: (record: SettleOrder) => void
}

export default function SettleOrderTable({
  data,
  loading,
  onDetail,
  onPageChange,
  onReceive,
  page,
  pageSize,
  total,
}: Props) {
  return (
    <Table<SettleOrder>
      className="mes-table-card"
      rowKey="uuid"
      size="small"
      loading={loading}
      columns={buildColumns({ onDetail, onReceive })}
      dataSource={data}
      pagination={{
        current: page,
        pageSize,
        total,
        showSizeChanger: true,
        pageSizeOptions: [10, 20, 50, 100, 200],
        showTotal: (value) => `共 ${value} 条`,
        onChange: onPageChange,
      }}
      scroll={{ x: 1020, y: 'calc(100vh - 600px)' }}
    />
  )
}

function buildColumns(actions: {
  onDetail: (record: SettleOrder) => void
  onReceive: (record: SettleOrder) => void
}): ColumnsType<SettleOrder> {
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
    { title: '客户', dataIndex: 'customerName', width: 140, ellipsis: true },
    { title: '结算日期', dataIndex: 'settleDate', width: 110 },
    { title: '应收', dataIndex: 'totalAmount', align: 'right', width: 110, render: (value) => formatMoney(value) },
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
      render: (value) => {
        const status = SETTLE_STATUS[value]
        return status ? <Tag className="mes-status-tag" color={status.color}>{status.text}</Tag> : '-'
      },
    },
    {
      title: '操作',
      key: 'actions',
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
