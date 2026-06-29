import { Button, Space, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { DeliveryOrder } from '../../../types/delivery'
import { DELIVERY_STATUS, SETTLE_BLOCK_ACTION } from '../../../constants/delivery'
import { formatKg } from '../utils/deliveryFormatters'

interface Props {
  data: DeliveryOrder[]
  loading: boolean
  page: number
  pageSize: number
  total: number
  onConfirm: (record: DeliveryOrder) => void
  onDetail: (record: DeliveryOrder) => void
  onPageChange: (page: number, pageSize: number) => void
}

export default function DeliveryOrderTable({
  data,
  loading,
  onConfirm,
  onDetail,
  onPageChange,
  page,
  pageSize,
  total,
}: Props) {
  const columns = buildColumns({ onConfirm, onDetail })
  return (
    <Table<DeliveryOrder>
      className="mes-table-card"
      rowKey="uuid"
      size="small"
      loading={loading}
      columns={columns}
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
      scroll={{ x: 980, y: 'calc(100vh - 590px)' }}
    />
  )
}

function buildColumns(actions: {
  onConfirm: (record: DeliveryOrder) => void
  onDetail: (record: DeliveryOrder) => void
}): ColumnsType<DeliveryOrder> {
  return [
    {
      title: '出库单号',
      dataIndex: 'deliveryNo',
      fixed: 'left',
      width: 150,
      render: (value) => <Typography.Text strong>{value}</Typography.Text>,
    },
    { title: '客户', dataIndex: 'customerName', width: 140, ellipsis: true },
    { title: '日期', dataIndex: 'deliveryDate', width: 110 },
    {
      title: '出库统计',
      dataIndex: 'totalCount',
      width: 150,
      render: (_, record) => (
        <div className="delivery-cell-stack mes-cell-stack">
          <span>{record.totalCount} 卷</span>
          <span>{formatKg(record.totalWeight)}</span>
        </div>
      ),
    },
    { title: '提货人', dataIndex: 'pickerName', width: 110, render: (value) => value || '-' },
    { title: '车牌/柜号', dataIndex: 'carNo', width: 145, render: (_, record) => `${record.carNo || '-'} / ${record.containerNo || '-'}` },
    {
      title: '状态',
      dataIndex: 'deliveryStatus',
      width: 105,
      render: (value) => {
        const status = DELIVERY_STATUS[value]
        return status ? <Tag className="mes-status-tag" color={status.color}>{status.text}</Tag> : '-'
      },
    },
    {
      title: '结算拦截',
      dataIndex: 'settleBlockAction',
      width: 110,
      render: (value) => (value ? <Tag className="mes-status-tag" color="orange">{SETTLE_BLOCK_ACTION[value]}</Tag> : '-'),
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
          {record.deliveryStatus === 1 && (
            <Button type="link" size="small" onClick={() => actions.onConfirm(record)}>
              签收
            </Button>
          )}
        </Space>
      ),
    },
  ]
}
