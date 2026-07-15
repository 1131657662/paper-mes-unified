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
      {sources.map((source, index) => (
        <div key={source.originalUuid ?? `${source.rollNo ?? 'source'}-${index}`}>
          <Typography.Text strong>{sourceLabel(source, index)}</Typography.Text>
          <span>{sourceDetailText(source)}</span>
        </div>
      ))}
    </div>
  )
}

function sourceLabel(source: FinishSourceVO, index: number) {
  return source.rollNo ? `卷号 ${source.rollNo}` : `母卷${source.rowSort ?? index + 1}`
}

function sourceDetailText(source: FinishSourceVO) {
  const identity = source.extraNo ? `编号 ${source.extraNo}` : undefined
  const gram = source.actualGramWeight ?? source.gramWeight
  const width = source.actualWidth ?? source.originalWidth
  const weight = source.actualWeight ?? source.totalWeight ?? sourceWeight(source)
  return [
    identity,
    source.paperName,
    gram == null ? undefined : formatGram(gram),
    width == null ? undefined : formatMm(width),
    weight == null ? undefined : formatKg(weight),
  ].filter(Boolean).join(' / ') || '-'
}

function sourceWeight(source: FinishSourceVO) {
  if (source.rollWeight == null) return undefined
  return source.rollWeight * (source.pieceNum ?? 1)
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
