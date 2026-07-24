import { Empty, Table, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { formatGram, formatKg, formatMm, formatTonFromKg } from '../../utils/numberFormatters'
import type { FinishedProductRow } from '../processOrderDetail/components/finishedProductRows'
import { buildCustomerSpecificationGroups, type CustomerSpecificationGroup } from './customerSpecModel'
import type { FinishCustomerSpec } from './customerSpecTypes'

interface Props { rows: FinishedProductRow[]; specs?: FinishCustomerSpec[] }

export default function CustomerSpecificationSummaryTable({ rows, specs }: Props) {
  const groups = buildCustomerSpecificationGroups(rows, specs)
  if (!groups.length) return <Empty description="暂无可提货成品" />
  const count = groups.reduce((sum, row) => sum + row.count, 0)
  const weight = groups.reduce((sum, row) => sum + row.weight, 0)
  return (
    <Table<CustomerSpecificationGroup>
      bordered className="finished-products-table mes-table-card" columns={columns}
      dataSource={groups} pagination={false} rowKey="key" size="small"
      summary={() => summary(count, weight)}
    />
  )
}

const columns: ColumnsType<CustomerSpecificationGroup> = [
  { title: '客户品名', dataIndex: 'paperName', width: 220, render: (value) => <Typography.Text strong>{value ?? '-'}</Typography.Text> },
  { title: '客户克重', dataIndex: 'gramWeight', align: 'right', width: 120, render: formatGram },
  { title: '客户门幅', dataIndex: 'width', align: 'right', width: 130, render: formatMm },
  { title: '件数', dataIndex: 'count', align: 'right', width: 100, render: (value) => `${value} 件` },
  { title: '客户单据重量', dataIndex: 'weight', align: 'right', width: 160, render: formatKg },
  { title: '现场实物规格', dataIndex: 'physicalSpecifications', render: renderPhysical },
]

function renderPhysical(values: string[]) {
  return <div className="customer-physical-specs">{values.map((value) => <span key={value}>{value}</span>)}</div>
}

function summary(count: number, weight: number) {
  return <Table.Summary.Row className="finished-products-summary"><Table.Summary.Cell index={0}>客户口径合计</Table.Summary.Cell><Table.Summary.Cell index={1} colSpan={2} /><Table.Summary.Cell index={3} align="right">{count} 件</Table.Summary.Cell><Table.Summary.Cell index={4} align="right">{formatTonFromKg(weight)}</Table.Summary.Cell><Table.Summary.Cell index={5} /></Table.Summary.Row>
}
