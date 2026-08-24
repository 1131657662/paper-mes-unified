import { Button, Space, Table, Typography } from 'antd'
import { CheckOutlined, CloseOutlined, CreditCardOutlined, LinkOutlined, UndoOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { RemainAdjustment } from '../../types/remain'
import { StatusTag, formatAmount, formatWeight } from './remainDisplay'

interface Props {
  rows: RemainAdjustment[]
  loading: boolean
  onNextSettlement: (row: RemainAdjustment) => void
  onCredit: (row: RemainAdjustment) => void
  onRefund: (row: RemainAdjustment) => void
  onCancel: (row: RemainAdjustment) => void
  onReverseCredit: (row: RemainAdjustment) => void
}

export function RemainAdjustmentTable({ rows, loading, onNextSettlement, onCredit, onRefund, onCancel, onReverseCredit }: Props) {
  const columns: ColumnsType<RemainAdjustment> = [
    { title: '调整单号', dataIndex: 'adjustmentNo', render: (value: string) => <Typography.Text strong>{value}</Typography.Text> },
    { title: '登记单', dataIndex: 'registrationUuid', ellipsis: true },
    { title: '客户', dataIndex: 'customerUuid', ellipsis: true },
    { title: '金额', dataIndex: 'amount', render: formatAmount },
    { title: '重量', dataIndex: 'weight', render: formatWeight },
    { title: '去向', dataIndex: 'targetType', render: (value: string) => <StatusTag value={value} /> },
    { title: '状态', dataIndex: 'status', render: (value: string) => <StatusTag value={value} /> },
    {
      title: '操作', key: 'actions', render: (_value, row) => {
        if (row.status === 'APPLIED' && row.targetType === 'CUSTOMER_CREDIT') {
          return <Button type="text" icon={<UndoOutlined />} onClick={() => onReverseCredit(row)}>冲回余款</Button>
        }
        if (row.status !== 'PENDING' || row.targetType !== 'PENDING') return null
        return (
          <Space size="small">
            <Button type="text" icon={<LinkOutlined />} onClick={() => onNextSettlement(row)}>挂接结算</Button>
            <Button type="text" icon={<CreditCardOutlined />} onClick={() => onCredit(row)}>入客户余款</Button>
            <Button type="text" icon={<CheckOutlined />} onClick={() => onRefund(row)}>申请退款</Button>
            <Button type="text" danger icon={<CloseOutlined />} onClick={() => onCancel(row)}>取消</Button>
          </Space>
        )
      },
    },
  ]
  return <Table rowKey="uuid" columns={columns} dataSource={rows} loading={loading} pagination={{ pageSize: 10 }} scroll={{ x: 1200 }} />
}
