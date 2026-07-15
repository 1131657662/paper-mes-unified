import { Empty, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { FinishProductionVO, FinishSourceVO } from '../../../types/processOrder'
import {
  formatGram,
  formatKg,
  formatMm,
  formatOptionalKg,
  formatPercent,
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
  { title: '来源母卷', width: 350, render: (_, row) => renderSources(row.sources) },
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
  const details = [
    finish.finishDiameter == null ? undefined : `直径 ${formatMm(finish.finishDiameter)}`,
    finish.finishCoreDiameter == null ? undefined : `纸芯 ${formatMm(finish.finishCoreDiameter)}`,
  ].filter(Boolean).join(' / ')
  return (
    <div className="finished-product-cell">
      <Typography.Text>{formatMm(finish.finishWidth)}</Typography.Text>
      {details && <span>{details}</span>}
    </div>
  )
}

function renderSources(sources: FinishSourceVO[]) {
  if (sources.length === 0) return '-'
  return (
    <div className="customer-source-rolls">
      {sources.map((source, index) => (
        <div key={source.originalUuid ?? `${source.rollNo ?? 'source'}-${index}`}>
          <Typography.Text strong>{source.rollNo ?? source.paperName ?? '-'}</Typography.Text>
          {source.paperName && source.paperName !== source.rollNo && <span>{source.paperName}</span>}
          <span>{sourceShareText(source)}</span>
        </div>
      ))}
    </div>
  )
}

function sourceShareText(source: FinishSourceVO) {
  const ratio = source.shareRatio == null ? '-' : formatPercent(source.shareRatio)
  return `分摊比例 ${ratio} · 分摊重量 ${formatOptionalKg(source.shareWeight)}`
}

function renderSummary(count: number, weight: number) {
  return (
    <Table.Summary.Row className="finished-products-summary finished-products-summary--customer">
      <Table.Summary.Cell index={0}>提货合计</Table.Summary.Cell>
      <Table.Summary.Cell index={1} colSpan={3}>{count} 件</Table.Summary.Cell>
      <Table.Summary.Cell index={4} align="right">{formatTonFromKg(weight)}</Table.Summary.Cell>
      <Table.Summary.Cell index={5} />
    </Table.Summary.Row>
  )
}
