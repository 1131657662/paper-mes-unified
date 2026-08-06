import { Empty, Table, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { SortOrder } from 'antd/es/table/interface'
import { DISPLAY_TERMS } from '../../constants/displayTerms'
import { formatGram, formatKg, formatMm, formatTonFromKg } from '../../utils/numberFormatters'
import type { FinishedProductRow } from '../processOrderDetail/components/finishedProductRows'
import { buildCustomerSpecificationGroups, type CustomerSpecificationGroup } from './customerSpecModel'
import type { FinishCustomerSpec } from './customerSpecTypes'
import type { FinishedProductsSortController } from '../processOrderDetail/components/useFinishedProductsSortState'
import { sortCustomerSummaryRows, type CustomerSummarySortField, type CustomerSummarySortSpec } from '../processOrderDetail/components/finishedProductsSorting'

interface Props { rows: FinishedProductRow[]; specs?: FinishCustomerSpec[]; sortState?: FinishedProductsSortController<CustomerSpecificationGroup, CustomerSummarySortSpec> }

export default function CustomerSpecificationSummaryTable({ rows, specs, sortState: providedSortState }: Props) {
  const sortState = providedSortState ?? emptySortState<CustomerSummarySortSpec>()
  const groups = buildCustomerSpecificationGroups(rows, specs)
  if (!groups.length) return <Empty description="暂无可提货成品" />
  const count = groups.reduce((sum, row) => sum + row.count, 0)
  const weight = groups.reduce((sum, row) => sum + row.weight, 0)
  return (
    <Table<CustomerSpecificationGroup>
      bordered className="finished-products-table mes-table-card" columns={buildColumns(sortState.sortChain)}
      dataSource={sortCustomerSummaryRows(groups, sortState.sortChain)} pagination={false} rowKey="key" size="small"
      onChange={sortState.onChange}
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

function buildColumns(sortChain: NonNullable<Props['sortState']>['sortChain']): ColumnsType<CustomerSpecificationGroup> {
  return columns.map((column) => {
    const field = 'dataIndex' in column && typeof column.dataIndex === 'string' ? column.dataIndex as CustomerSummarySortField : undefined
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

function renderPhysical(values: string[]) {
  return <div className="customer-physical-specs">{values.map((value) => <span key={value}>{value}</span>)}</div>
}

function emptySortState<TSort>(): FinishedProductsSortController<CustomerSpecificationGroup, TSort> {
  return { sortChain: [], onChange: () => undefined, clearSort: () => undefined }
}

function summary(count: number, weight: number) {
  return <Table.Summary.Row className="finished-products-summary"><Table.Summary.Cell index={0}>{DISPLAY_TERMS.customerSpecification}合计</Table.Summary.Cell><Table.Summary.Cell index={1} colSpan={2} /><Table.Summary.Cell index={3} align="right">{count} 件</Table.Summary.Cell><Table.Summary.Cell index={4} align="right">{formatTonFromKg(weight)}</Table.Summary.Cell><Table.Summary.Cell index={5} /></Table.Summary.Row>
}
