import { Empty, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { formatKg, formatWholeKg } from '../../utils/numberFormatters'
import type { FinishedProductRow } from '../processOrderDetail/components/finishedProductRows'
import { customerSpecificationLabel, physicalSpecificationLabel } from './customerSpecModel'
import type { FinishCustomerSpec } from './customerSpecTypes'

interface Props { rows: FinishedProductRow[]; specs?: FinishCustomerSpec[] }
interface ComparisonRow { row: FinishedProductRow; spec: FinishCustomerSpec }

export default function CustomerSpecificationComparisonTable({ rows, specs = [] }: Props) {
  const index = new Map(rows.map((row) => [row.finish.uuid, row]))
  const data = specs.flatMap((spec) => index.has(spec.finishUuid) ? [{ row: index.get(spec.finishUuid)!, spec }] : [])
  if (!data.length) return <Empty description="暂无逐件明细" />
  return <Table<ComparisonRow> bordered className="finished-products-table mes-table-card" columns={columns} dataSource={data} pagination={false} rowKey={({ spec }) => spec.finishUuid} scroll={{ x: 920 }} size="small" />
}

const columns: ColumnsType<ComparisonRow> = [
  { title: '成品卷号', width: 150, fixed: 'left', render: (_, { spec }) => <Typography.Text strong>{spec.finishRollNo ?? '-'}</Typography.Text> },
  { title: '客户标签', width: 280, render: (_, { spec }) => <SpecificationCell primary={customerSpecificationLabel(spec)} secondary={`现场：${physicalSpecificationLabel(spec)}`} changed={spec.specificationChanged} /> },
  { title: '客户单据重量', width: 170, align: 'right', render: (_, { spec }) => <WeightCell spec={spec} /> },
  { title: '来源母卷', width: 220, render: (_, { row }) => row.sources.map((source) => source.rollNo || source.extraNo || source.paperName).filter(Boolean).join('、') || '-' },
  { title: '状态', width: 100, render: (_, { spec }) => spec.specificationChanged || spec.weightChanged ? <Tag color="blue">已转换</Tag> : <Tag>一致</Tag> },
]

function SpecificationCell({ primary, secondary, changed }: { primary: string; secondary: string; changed: boolean }) {
  return <div className="customer-comparison-cell"><Typography.Text strong>{primary || '-'}</Typography.Text><span>{secondary}</span>{changed && <Tag color="gold">标签有调整</Tag>}</div>
}

function WeightCell({ spec }: { spec: FinishCustomerSpec }) {
  return <div className="customer-comparison-cell is-right"><Typography.Text strong>{formatWholeKg(spec.customerDisplayWeight)}</Typography.Text><span>实物：{formatKg(spec.physicalWeight)}</span></div>
}
