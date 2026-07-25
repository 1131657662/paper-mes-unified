import { Empty, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { FinishProductionVO, FinishSourceVO } from '../../../types/processOrder'
import {
  formatGram,
  formatKg,
  formatMm,
  formatTonFromKg,
} from '../../../utils/numberFormatters'
import {
  customerFinishedProductWeight,
  isCustomerFinishedProductVisible,
} from './customerFinishedProductRows'
import type { FinishedProductRow } from './finishedProductRows'

interface Props {
  rows: FinishedProductRow[]
}

export default function CustomerFinishedProductDetailTable({ rows }: Props) {
  const visibleRows = rows.filter(isCustomerFinishedProductVisible)
  const totalWeight = visibleRows.reduce((sum, row) => sum + customerFinishedProductWeight(row), 0)
  if (visibleRows.length === 0) return <Empty description="暂无可提货成品" />
  return (
    <Table<FinishedProductRow>
      bordered
      className="finished-products-table finished-products-table--customer mes-table-card"
      columns={columns}
      dataSource={visibleRows}
      pagination={false}
      rowKey="key"
      scroll={{ x: 1180 }}
      size="small"
      summary={() => renderSummary(visibleRows.length, totalWeight)}
    />
  )
}

const columns: ColumnsType<FinishedProductRow> = [
  { title: '成品卷号', width: 170, fixed: 'left', render: (_, row) => <Typography.Text strong>{row.finish.finishRollNo ?? '-'}</Typography.Text> },
  { title: '品名', width: 200, render: (_, row) => renderName(row.finish) },
  { title: '克重', align: 'right', width: 100, render: (_, row) => formatGram(row.finish.gramWeight) },
  { title: '规格', width: 220, render: (_, row) => renderSpec(row.finish) },
  { title: '重量', align: 'right', width: 140, render: (_, row) => formatKg(customerFinishedProductWeight(row)) },
  { title: '来源母卷', width: 390, render: (_, row) => renderSources(row.sources) },
  { title: '备注', width: 180, render: (_, row) => row.finish.actualRemark || '-' },
]

function renderName(finish: FinishProductionVO) {
  return (
    <div className="finished-product-name">
      <Typography.Text strong>{finish.paperName ?? '-'}</Typography.Text>
      {finish.isRemain === 1 && <Tag color="orange">切边</Tag>}
    </div>
  )
}

function renderSpec(finish: FinishProductionVO) {
  return <Typography.Text>{formatMm(finish.finishWidth)}</Typography.Text>
}

function renderSources(sources: FinishSourceVO[]) {
  if (sources.length === 0) return '-'
  return (
    <div className="customer-source-rolls">
      {sources.map((source) => (
        <div key={sourceKey(source)}>
          <div className="customer-source-rolls__spec">
            <Typography.Text strong>{source.paperName ?? '-'}</Typography.Text>
            <span>{sourceGram(source)}</span>
            <span>{sourceWidth(source)}</span>
          </div>
          <div className="customer-source-rolls__identity">
            <span>{sourceRollNo(source)}</span>
            <span>编号 {source.extraNo ?? '-'}</span>
            <span>{sourcePieceWeightText(source)}</span>
          </div>
        </div>
      ))}
    </div>
  )
}

function sourceRollNo(source: FinishSourceVO) {
  return `卷号 ${source.rollNo ?? '-'}`
}

function sourceKey(source: FinishSourceVO) {
  return source.originalUuid ?? source.rollNo ?? source.extraNo
    ?? `${source.paperName ?? '-'}-${source.gramWeight ?? '-'}-${source.originalWidth ?? '-'}`
}

function sourceGram(source: FinishSourceVO) {
  const gram = source.actualGramWeight ?? source.gramWeight
  return gram == null ? '-' : formatGram(gram)
}

function sourceWidth(source: FinishSourceVO) {
  const width = source.actualWidth ?? source.originalWidth
  return width == null ? '-' : formatMm(width)
}

function sourcePieceWeight(source: FinishSourceVO) {
  if (source.rollWeight != null) return source.rollWeight
  const count = Math.max(1, source.pieceNum ?? 1)
  if (source.actualWeight != null) return source.actualWeight / count
  if (source.totalWeight != null) return source.totalWeight / count
  return undefined
}

function sourcePieceWeightText(source: FinishSourceVO) {
  const weight = sourcePieceWeight(source)
  return `件重 ${weight == null ? '-' : formatKg(weight)}`
}

function renderSummary(count: number, weight: number) {
  return (
    <Table.Summary.Row className="finished-products-summary finished-products-summary--customer">
      <Table.Summary.Cell index={0}>提货合计</Table.Summary.Cell>
      <Table.Summary.Cell index={1} colSpan={3}>{count} 件</Table.Summary.Cell>
      <Table.Summary.Cell index={4} align="right">{formatTonFromKg(weight)}</Table.Summary.Cell>
      <Table.Summary.Cell index={5} />
      <Table.Summary.Cell index={6} />
    </Table.Summary.Row>
  )
}
