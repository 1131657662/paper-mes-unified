import { Empty, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { SortOrder } from 'antd/es/table/interface'
import { formatGram, formatKg, formatMm, formatOptionalKg } from '../../../utils/numberFormatters'
import type { FinishedProductRow } from './finishedProductRows'
import {
  buildPhysicalSpecificationGroups,
  calculatePhysicalSpecificationTotals,
  type PhysicalProductType,
  type PhysicalSpecificationGroup,
  type PhysicalSpecificationTotals,
} from './physicalSpecificationModel'
import type { FinishedProductsSortController } from './useFinishedProductsSortState'
import { sortPhysicalSummaryRows, type PhysicalSummarySortField, type PhysicalSummarySortSpec } from './finishedProductsSorting'

interface Props { rows: FinishedProductRow[]; sortState?: FinishedProductsSortController<PhysicalSpecificationGroup, PhysicalSummarySortSpec> }

export default function PhysicalSpecificationSummaryTable({ rows, sortState: providedSortState }: Props) {
  const sortState = providedSortState ?? emptySortState()
  const groups = buildPhysicalSpecificationGroups(rows)
  if (!groups.length) return <Empty description="暂无有效实物成品" />
  const totals = calculatePhysicalSpecificationTotals(groups)
  return (
    <Table<PhysicalSpecificationGroup>
      bordered className="finished-products-table mes-table-card" columns={buildColumns(sortState.sortChain)}
      dataSource={sortPhysicalSummaryRows(groups, sortState.sortChain)} pagination={false} rowKey="key" scroll={{ x: 840 }} size="small"
      onChange={sortState.onChange}
      summary={() => renderSummary(totals)}
    />
  )
}

const columns: ColumnsType<PhysicalSpecificationGroup> = [
  { title: '实物品名', dataIndex: 'paperName', width: 150, render: (value) => <Typography.Text strong>{value ?? '-'}</Typography.Text> },
  { title: '克重', dataIndex: 'gramWeight', align: 'right', width: 80, render: formatGram },
  { title: '门幅', dataIndex: 'width', align: 'right', width: 90, render: formatMm },
  { title: '类型', dataIndex: 'productType', width: 75, render: renderType },
  { title: '件数 / 回录', width: 115, align: 'right', render: (_, row) => `${row.count} 件 / ${row.recordedCount} 件` },
  { title: '预估重量', dataIndex: 'estimateWeight', width: 110, align: 'right', render: formatKg },
  { title: '实际重量', dataIndex: 'actualWeight', width: 110, align: 'right', render: formatOptionalKg },
  { title: '差异', dataIndex: 'difference', width: 110, align: 'right', render: renderDifference },
]

function buildColumns(sortChain: PhysicalSummarySortSpec[]): ColumnsType<PhysicalSpecificationGroup> {
  return columns.map((column, index) => {
    const field = physicalSummaryFields[index]
    if (!field) return column
    const active = sortChain.find((item) => item.field === field)
    const priority = sortChain.findIndex((item) => item.field === field)
    return {
      ...column,
      key: field,
      title: priority >= 0 && typeof column.title === 'string' ? `${column.title} ${priority + 1}` : column.title,
      sorter: { multiple: 1 },
      sortOrder: (active?.direction === 'asc' ? 'ascend' : active?.direction === 'desc' ? 'descend' : null) as SortOrder,
      sortDirections: ['ascend', 'descend', null] as SortOrder[],
    }
  })
}

const physicalSummaryFields: PhysicalSummarySortField[] = ['paperName', 'gramWeight', 'width', 'productType', 'count', 'estimateWeight', 'actualWeight', 'difference']

function emptySortState(): FinishedProductsSortController<PhysicalSpecificationGroup, PhysicalSummarySortSpec> {
  return { sortChain: [], onChange: () => undefined, clearSort: () => undefined }
}

function renderType(value: PhysicalProductType) {
  const types = {
    FINISH: { color: 'success', text: '正式' },
    SPARE: { color: 'gold', text: '备用' },
    TRIM: { color: 'orange', text: '切边' },
  } satisfies Record<PhysicalProductType, { color: string; text: string }>
  return <Tag color={types[value].color}>{types[value].text}</Tag>
}

function renderDifference(value?: number) {
  if (value == null) return <Typography.Text type="secondary">待回录完整</Typography.Text>
  if (value === 0) return formatKg(0)
  const className = value > 0 ? 'is-positive' : 'is-negative'
  return <span className={className}>{value > 0 ? '+' : '-'}{formatKg(Math.abs(value))}</span>
}

function renderSummary(totals: PhysicalSpecificationTotals) {
  return (
    <Table.Summary.Row className="finished-products-summary">
      <Table.Summary.Cell index={0}>实物合计</Table.Summary.Cell>
      <Table.Summary.Cell index={1} colSpan={3} />
      <Table.Summary.Cell index={4} align="right">{totals.count} 件 / {totals.recordedCount} 件</Table.Summary.Cell>
      <Table.Summary.Cell index={5} align="right">{formatKg(totals.estimateWeight)}</Table.Summary.Cell>
      <Table.Summary.Cell index={6} align="right">{formatOptionalKg(totals.actualWeight)}</Table.Summary.Cell>
      <Table.Summary.Cell index={7} align="right">{renderDifference(totals.difference)}</Table.Summary.Cell>
    </Table.Summary.Row>
  )
}
