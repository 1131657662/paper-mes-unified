import { Button, Space, Table, Typography } from 'antd'
import { CheckOutlined, CloseOutlined, DollarOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { RemainRefund } from '../../types/remain'
import { StatusTag, formatAmount, formatWeight } from './remainDisplay'

interface Props {
  rows: RemainRefund[]
  loading: boolean
  onApprove: (row: RemainRefund) => void
  onPay: (row: RemainRefund) => void
  onCancel: (row: RemainRefund) => void
}

export function RemainRefundTable({ rows, loading, onApprove, onPay, onCancel }: Props) {
  const columns: ColumnsType<RemainRefund> = [
    { title: '退款单号', dataIndex: 'refundNo', render: (value: string) => <Typography.Text strong>{value}</Typography.Text> },
    { title: '调整单', dataIndex: 'adjustmentUuid', ellipsis: true },
    { title: '客户', dataIndex: 'customerUuid', ellipsis: true },
    { title: '金额', dataIndex: 'amount', render: formatAmount },
    { title: '重量', dataIndex: 'weight', render: formatWeight },
    { title: '状态', dataIndex: 'status', render: (value: string) => <StatusTag value={value} /> },
    { title: '支付凭证', dataIndex: 'paymentReference', ellipsis: true },
    {
      title: '操作', key: 'actions', render: (_value, row) => (
        <Space size="small">
          {row.status === 'REQUESTED' && <Button type="text" icon={<CheckOutlined />} onClick={() => onApprove(row)}>审批</Button>}
          {row.status === 'APPROVED' && <Button type="text" icon={<DollarOutlined />} onClick={() => onPay(row)}>支付</Button>}
          {(row.status === 'REQUESTED' || row.status === 'APPROVED') && <Button type="text" danger icon={<CloseOutlined />} onClick={() => onCancel(row)}>取消</Button>}
        </Space>
      ),
    },
  ]
  return <Table rowKey="uuid" columns={columns} dataSource={rows} loading={loading} pagination={{ pageSize: 10 }} scroll={{ x: 1000 }} />
}
