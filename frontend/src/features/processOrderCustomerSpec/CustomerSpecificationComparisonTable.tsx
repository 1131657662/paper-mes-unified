import { Empty, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { SortOrder } from 'antd/es/table/interface'
import type { FinishSourceVO } from '../../types/processOrder'
import { formatGram, formatKg, formatMm, formatWholeKg } from '../../utils/numberFormatters'
import type { FinishedProductRow } from '../processOrderDetail/components/finishedProductRows'
import { customerSpecificationLabel, physicalSpecificationLabel } from './customerSpecModel'
import type { FinishCustomerSpec } from './customerSpecTypes'
import type { FinishedProductsSortController } from '../processOrderDetail/components/useFinishedProductsSortState'
import {
  sortCustomerComparisonRows,
  type CustomerComparisonSortField,
  type CustomerComparisonSortSpec,
  type CustomerSpecificationComparisonRow,
} from '../processOrderDetail/components/finishedProductsSorting'

interface Props { rows: FinishedProductRow[]; specs?: FinishCustomerSpec[]; sortState?: FinishedProductsSortController<CustomerSpecificationComparisonRow, CustomerComparisonSortSpec> }

export default function CustomerSpecificationComparisonTable({ rows, specs = [], sortState: providedSortState }: Props) {
  const sortState = providedSortState ?? emptySortState()
  const index = new Map(rows.map((row) => [row.finish.uuid, row]))
  const data = specs.flatMap((spec) => index.has(spec.finishUuid) ? [{ row: index.get(spec.finishUuid)!, spec }] : [])
  if (!data.length) return <Empty description="暂无逐件明细" />
  return <Table<CustomerSpecificationComparisonRow> bordered className="finished-products-table mes-table-card" columns={buildColumns(sortState.sortChain)} dataSource={sortCustomerComparisonRows(data, sortState.sortChain)} pagination={false} rowKey={({ spec }) => spec.finishUuid} scroll={{ x: 1090 }} size="small" onChange={sortState.onChange} />
}

const columns: ColumnsType<CustomerSpecificationComparisonRow> = [
  { title: '成品卷号', width: 150, fixed: 'left', render: (_, { spec }) => <Typography.Text strong>{spec.finishRollNo ?? '-'}</Typography.Text> },
  { title: '客户标签', width: 280, render: (_, { spec }) => <SpecificationCell primary={customerSpecificationLabel(spec)} secondary={`现场：${physicalSpecificationLabel(spec)}`} changed={spec.specificationChanged} /> },
  { title: '客户单据重量', width: 170, align: 'right', render: (_, { spec }) => <WeightCell spec={spec} /> },
  { title: '来源母卷', width: 390, render: (_, { row }) => <SourceRollCell sources={row.sources} /> },
  { title: '状态', width: 100, render: (_, { spec }) => spec.specificationChanged || spec.weightChanged ? <Tag color="blue">已转换</Tag> : <Tag>一致</Tag> },
]

const comparisonFields: CustomerComparisonSortField[] = ['finishRollNo', 'customerSpecification', 'customerDisplayWeight', 'sourceMotherRoll', 'status']

function emptySortState(): FinishedProductsSortController<CustomerSpecificationComparisonRow, CustomerComparisonSortSpec> {
  return { sortChain: [], onChange: () => undefined, clearSort: () => undefined }
}

function buildColumns(sortChain: CustomerComparisonSortSpec[]): ColumnsType<CustomerSpecificationComparisonRow> {
  return columns.map((column, index) => {
    const field = comparisonFields[index]
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

function SpecificationCell({ primary, secondary, changed }: { primary: string; secondary: string; changed: boolean }) {
  return <div className="customer-comparison-cell"><Typography.Text strong>{primary || '-'}</Typography.Text><span>{secondary}</span>{changed && <Tag color="gold">标签有调整</Tag>}</div>
}

function WeightCell({ spec }: { spec: FinishCustomerSpec }) {
  return <div className="customer-comparison-cell is-right"><Typography.Text strong>{formatWholeKg(spec.customerDisplayWeight)}</Typography.Text><span>实物：{formatKg(spec.physicalWeight)}</span></div>
}

function SourceRollCell({ sources }: { sources: FinishSourceVO[] }) {
  if (sources.length === 0) return '-'
  return <div className="customer-source-rolls">{sources.map((source) => (
    <div key={sourceKey(source)}>
      <div className="customer-source-rolls__spec">
        <Typography.Text strong>{source.paperName ?? '-'}</Typography.Text>
        <span>克重 {sourceGram(source)}</span>
        <span>门幅 {sourceWidth(source)}</span>
      </div>
      <div className="customer-source-rolls__identity">
        <span>卷号 {source.rollNo ?? '-'}</span>
        <span>编号 {source.extraNo ?? '-'}</span>
        <span>件重 {formatSourcePieceWeight(source)}</span>
      </div>
    </div>
  ))}</div>
}

function sourceKey(source: FinishSourceVO) {
  return source.originalUuid ?? source.rollNo ?? source.extraNo
    ?? `${source.paperName ?? '-'}-${source.gramWeight ?? '-'}-${source.originalWidth ?? '-'}`
}

function sourceGram(source: FinishSourceVO) {
  return formatGram(source.actualGramWeight ?? source.gramWeight)
}

function sourceWidth(source: FinishSourceVO) {
  return formatMm(source.actualWidth ?? source.originalWidth)
}

function formatSourcePieceWeight(source: FinishSourceVO) {
  const weight = sourcePieceWeight(source)
  return weight == null ? '-' : formatKg(weight)
}

function sourcePieceWeight(source: FinishSourceVO) {
  if (source.rollWeight != null) return source.rollWeight
  const count = Math.max(1, source.pieceNum ?? 1)
  if (source.actualWeight != null) return source.actualWeight / count
  if (source.totalWeight != null) return source.totalWeight / count
  return undefined
}
