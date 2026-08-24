import { Table, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { TableRowSelection } from 'antd/es/table/interface'
import type { RemainInventory } from '../../types/remain'
import { StatusTag, formatWeight } from './remainDisplay'

interface Props {
  rows: RemainInventory[]
  loading: boolean
  selectedRowKeys?: string[]
  onSelectionChange?: (rows: RemainInventory[]) => void
}

export function RemainInventoryTable({ rows, loading, selectedRowKeys = [], onSelectionChange }: Props) {
  const columns: ColumnsType<RemainInventory> = [
    { title: '登记单号', dataIndex: 'registrationNo', render: (value: string) => <Typography.Text strong>{value}</Typography.Text> },
    { title: '来源余卷', dataIndex: 'sourceFinishRollUuid', ellipsis: true },
    { title: '客户', dataIndex: 'customerUuid', ellipsis: true },
    { title: '仓库', dataIndex: 'warehouseUuid', ellipsis: true },
    { title: '当前重量', dataIndex: 'currentWeight', render: formatWeight },
    { title: '库存状态', dataIndex: 'status', render: (value: string) => <StatusTag value={value} /> },
    { title: '价格状态', dataIndex: 'priceStatus', render: (value: string) => <StatusTag value={value} /> },
  ]
  const rowSelection: TableRowSelection<RemainInventory> | undefined = onSelectionChange ? {
    selectedRowKeys,
    onChange: (_, selectedRows) => onSelectionChange(selectedRows),
  } : undefined
  return <Table rowKey="lotUuid" rowSelection={rowSelection} columns={columns} dataSource={rows} loading={loading} pagination={{ pageSize: 10 }} scroll={{ x: 900 }} />
}
