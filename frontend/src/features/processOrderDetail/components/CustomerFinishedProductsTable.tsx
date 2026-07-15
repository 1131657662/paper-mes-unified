import { Empty, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import {
  formatGram,
  formatKg,
  formatMm,
  formatTonFromKg,
} from '../../../utils/numberFormatters'
import {
  buildCustomerFinishedProductRows,
  calculateCustomerFinishedProductTotals,
  type CustomerFinishedProductRow,
} from './customerFinishedProductRows'
import type { FinishedProductRow } from './finishedProductRows'

interface Props {
  rows: FinishedProductRow[]
}

export default function CustomerFinishedProductsTable({ rows }: Props) {
  const customerRows = buildCustomerFinishedProductRows(rows)
  const totals = calculateCustomerFinishedProductTotals(customerRows)
  if (customerRows.length === 0) return <Empty description="暂无可提货成品" />

  return (
    <Table<CustomerFinishedProductRow>
      bordered
      className="finished-products-table finished-products-table--customer mes-table-card"
      columns={columns}
      dataSource={customerRows}
      pagination={false}
      rowKey="key"
      size="small"
      summary={() => renderSummary(totals)}
    />
  )
}

const columns: ColumnsType<CustomerFinishedProductRow> = [
  {
    title: '品名',
    dataIndex: 'paperName',
    width: 220,
    render: (value, row) => (
      <div className="finished-product-name">
        <Typography.Text strong>{value ?? '-'}</Typography.Text>
        {row.isTrim && <Tag color="orange">切边</Tag>}
      </div>
    ),
  },
  {
    title: '克重',
    dataIndex: 'gramWeight',
    align: 'right',
    width: 110,
    render: formatGram,
  },
  {
    title: '规格',
    width: 230,
    render: (_, row) => renderSpec(row),
  },
  {
    title: '件数',
    dataIndex: 'count',
    align: 'right',
    width: 100,
    render: (value) => `${value} 件`,
  },
  {
    title: '重量',
    dataIndex: 'weight',
    align: 'right',
    width: 140,
    render: formatKg,
  },
]

function renderSpec(row: CustomerFinishedProductRow) {
  const detail = [
    row.diameter == null ? undefined : `直径 ${formatMm(row.diameter)}`,
    row.coreDiameter == null ? undefined : `纸芯 ${formatMm(row.coreDiameter)}`,
  ].filter(Boolean).join(' / ')
  return (
    <div className="finished-product-cell">
      <Typography.Text>{formatMm(row.width)}</Typography.Text>
      {detail && <span>{detail}</span>}
    </div>
  )
}

function renderSummary(totals: ReturnType<typeof calculateCustomerFinishedProductTotals>) {
  return (
    <Table.Summary.Row className="finished-products-summary finished-products-summary--customer">
      <Table.Summary.Cell index={0}>提货合计</Table.Summary.Cell>
      <Table.Summary.Cell index={1} colSpan={2} />
      <Table.Summary.Cell index={3} align="right">{totals.count} 件</Table.Summary.Cell>
      <Table.Summary.Cell index={4} align="right">{formatTonFromKg(totals.weight)}</Table.Summary.Cell>
    </Table.Summary.Row>
  )
}
