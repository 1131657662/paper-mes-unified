import { Button, Table, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { CloseOutlined } from '@ant-design/icons'
import type { RemainSale } from '../../types/remain'
import { formatAmount, formatWeight, StatusTag } from './remainDisplay'

export function RemainSaleTable({ rows, loading, onReverse }: { rows: RemainSale[]; loading: boolean; onReverse: (row: RemainSale) => void }) {
  const columns: ColumnsType<RemainSale> = [
    { title: '处理单号', dataIndex: 'saleNo', render: (value: string) => <Typography.Text strong>{value}</Typography.Text> },
    { title: '处理日期', dataIndex: 'processDate' },
    { title: '计价方式', dataIndex: 'pricingMode' },
    { title: '系统重量', dataIndex: 'systemWeight', render: formatWeight },
    { title: '计算金额', dataIndex: 'calculatedAmount', render: formatAmount },
    { title: '实收金额', dataIndex: 'receivedAmount', render: formatAmount },
    { title: '状态', dataIndex: 'status', render: (value: string) => <StatusTag value={value} /> },
    { title: '操作', key: 'actions', render: (_: unknown, row) => row.saleKind === 'SALE' && row.status === 'CONFIRMED' ? <Button type="text" danger icon={<CloseOutlined />} onClick={() => onReverse(row)}>作废</Button> : null },
  ]
  return <Table rowKey="uuid" columns={columns} dataSource={rows} loading={loading} pagination={{ pageSize: 10 }} scroll={{ x: 1000 }} />
}
