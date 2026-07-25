import { Empty, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { FinishSourceVO } from '../../types/processOrder'
import { formatGram, formatKg, formatMm, formatWholeKg } from '../../utils/numberFormatters'
import type { FinishedProductRow } from '../processOrderDetail/components/finishedProductRows'
import { customerSpecificationLabel, physicalSpecificationLabel } from './customerSpecModel'
import type { FinishCustomerSpec } from './customerSpecTypes'

interface Props { rows: FinishedProductRow[]; specs?: FinishCustomerSpec[] }
interface ComparisonRow { row: FinishedProductRow; spec: FinishCustomerSpec }

export default function CustomerSpecificationComparisonTable({ rows, specs = [] }: Props) {
  const index = new Map(rows.map((row) => [row.finish.uuid, row]))
  const data = specs.flatMap((spec) => index.has(spec.finishUuid) ? [{ row: index.get(spec.finishUuid)!, spec }] : [])
  if (!data.length) return <Empty description="暂无逐件明细" />
  return <Table<ComparisonRow> bordered className="finished-products-table mes-table-card" columns={columns} dataSource={data} pagination={false} rowKey={({ spec }) => spec.finishUuid} scroll={{ x: 1090 }} size="small" />
}

const columns: ColumnsType<ComparisonRow> = [
  { title: '成品卷号', width: 150, fixed: 'left', render: (_, { spec }) => <Typography.Text strong>{spec.finishRollNo ?? '-'}</Typography.Text> },
  { title: '客户标签', width: 280, render: (_, { spec }) => <SpecificationCell primary={customerSpecificationLabel(spec)} secondary={`现场：${physicalSpecificationLabel(spec)}`} changed={spec.specificationChanged} /> },
  { title: '客户单据重量', width: 170, align: 'right', render: (_, { spec }) => <WeightCell spec={spec} /> },
  { title: '来源母卷', width: 390, render: (_, { row }) => <SourceRollCell sources={row.sources} /> },
  { title: '状态', width: 100, render: (_, { spec }) => spec.specificationChanged || spec.weightChanged ? <Tag color="blue">已转换</Tag> : <Tag>一致</Tag> },
]

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
