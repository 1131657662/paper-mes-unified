import { EyeOutlined, RollbackOutlined, DollarOutlined } from '@ant-design/icons'
import { Button, Space, Table, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { RemainRegistration } from '../../types/remain'
import { StatusTag, formatAmount, formatWeight } from './remainDisplay'

interface Props {
  rows: RemainRegistration[]
  loading: boolean
  onDetail: (row: RemainRegistration) => void
  onPrice: (row: RemainRegistration) => void
  onRollback: (row: RemainRegistration) => void
}

export function RemainRegistrationTable({ rows, loading, onDetail, onPrice, onRollback }: Props) {
  const columns: ColumnsType<RemainRegistration> = [
    { title: '登记单号', dataIndex: 'registrationNo', render: (value: string) => <Typography.Text strong>{value}</Typography.Text> },
    { title: '加工单', dataIndex: 'orderUuid', ellipsis: true },
    { title: '客户', dataIndex: 'customerUuid', ellipsis: true },
    { title: '转入重量', dataIndex: 'totalTransferredWeight', render: formatWeight },
    { title: '已处理', dataIndex: 'totalProcessedWeight', render: formatWeight },
    { title: '金额', dataIndex: 'totalAmount', render: formatAmount },
    { title: '转让状态', dataIndex: 'status', render: (value: string) => <StatusTag value={value} /> },
    { title: '价格', dataIndex: 'priceStatus', render: (value: string) => <StatusTag value={value} /> },
    {
      title: '操作',
      key: 'actions',
      render: (_value, row) => (
        <Space size="small">
          <Button type="text" icon={<EyeOutlined />} onClick={() => onDetail(row)}>详情</Button>
          {row.priceStatus !== 'CONFIRMED' && row.status !== 'FULL_ROLLED_BACK' && (
            <Button type="text" icon={<DollarOutlined />} onClick={() => onPrice(row)}>定价</Button>
          )}
          {row.status !== 'FULL_ROLLED_BACK' && (
            <Button type="text" danger icon={<RollbackOutlined />} onClick={() => onRollback(row)}>回滚</Button>
          )}
        </Space>
      ),
    },
  ]
  return <Table rowKey="uuid" columns={columns} dataSource={rows} loading={loading} pagination={{ pageSize: 10 }} scroll={{ x: 1100 }} />
}
