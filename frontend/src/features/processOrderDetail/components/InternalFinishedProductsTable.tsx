import { Empty, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { FINISH_STATUS, ROLL_NO_STATUS } from '../../../constants/processOrder'
import type { FinishProductionVO, FinishSourceVO } from '../../../types/processOrder'
import { formatGram, formatKg, formatMm, formatOptionalKg, formatPercent } from '../../../utils/numberFormatters'
import { calculateFinishedProductTotals, type FinishedProductRow } from './finishedProductRows'

interface Props {
  rows: FinishedProductRow[]
}

export default function InternalFinishedProductsTable({ rows }: Props) {
  const totals = calculateFinishedProductTotals(rows)
  if (rows.length === 0) return <Empty description="暂无成品产出数据" />
  return (
    <Table<FinishedProductRow>
      bordered
      className="finished-products-table mes-table-card"
      columns={columns}
      dataSource={rows}
      pagination={false}
      rowKey="key"
      scroll={{ x: 1210 }}
      size="small"
      summary={() => renderSummary(totals)}
    />
  )
}

const columns: ColumnsType<FinishedProductRow> = [
  { title: '成品卷号', width: 150, fixed: 'left', render: (_, row) => <Typography.Text strong>{row.finish.finishRollNo ?? '-'}</Typography.Text> },
  { title: '成品规格', width: 230, render: (_, row) => renderSpec(row.finish) },
  { title: '来源母卷 / 分摊', width: 270, render: (_, row) => renderSources(row.sources) },
  { title: '类型 / 状态', width: 210, render: (_, row) => renderStatus(row.finish) },
  { title: '预估重量', align: 'right', width: 115, render: (_, row) => formatOptionalKg(row.finish.estimateWeight) },
  { title: '实际重量', align: 'right', width: 115, render: (_, row) => formatOptionalKg(row.finish.actualWeight) },
  { title: '差异', align: 'right', width: 120, render: (_, row) => renderDifference(row.finish) },
]

function renderSpec(finish: FinishProductionVO) {
  const diameter = finish.finishDiameter == null ? '-' : `直径 ${formatMm(finish.finishDiameter)}`
  const core = finish.finishCoreDiameter == null ? '-' : `纸芯 ${formatMm(finish.finishCoreDiameter)}`
  return (
    <div className="finished-product-cell">
      <Typography.Text>{finish.paperName ?? '-'}</Typography.Text>
      <span>{formatGram(finish.gramWeight)} / {formatMm(finish.finishWidth)}</span>
      <span>{diameter} · {core}</span>
    </div>
  )
}

function renderSources(sources: FinishSourceVO[]) {
  if (sources.length === 0) return '-'
  return (
    <div className="finished-product-sources">
      {sources.map((source, index) => (
        <div key={source.originalUuid ?? `${source.rollNo ?? 'source'}-${index}`}>
          <Typography.Text>{source.rollNo ?? source.paperName ?? '-'}</Typography.Text>
          <span>{sourceShareText(source)}</span>
        </div>
      ))}
    </div>
  )
}

function sourceShareText(source: FinishSourceVO) {
  const ratio = source.shareRatio == null ? '-' : formatPercent(source.shareRatio)
  return `分摊 ${ratio} / ${formatOptionalKg(source.shareWeight)}`
}

function renderStatus(finish: FinishProductionVO) {
  const rollStatus = finish.rollNoStatus == null ? undefined : ROLL_NO_STATUS[finish.rollNoStatus]
  return (
    <div className="finished-product-tags">
      <Tag color={typeColor(finish)}>{typeText(finish)}</Tag>
      <Tag color={finish.sourceType === 2 ? 'green' : 'blue'}>{finish.sourceType === 2 ? '直发' : '加工'}</Tag>
      {rollStatus && <Tag color={rollStatus.color}>{rollStatus.text}</Tag>}
      {finish.finishStatus != null && <Tag>{FINISH_STATUS[finish.finishStatus] ?? '未知状态'}</Tag>}
    </div>
  )
}

function typeText(finish: FinishProductionVO) {
  if (finish.isRemain === 1) return '切边'
  if (finish.isSpare === 1) return '备用'
  return '正式'
}

function typeColor(finish: FinishProductionVO) {
  if (finish.isRemain === 1) return 'orange'
  if (finish.isSpare === 1) return 'gold'
  return 'success'
}

function renderDifference(finish: FinishProductionVO) {
  if (finish.actualWeight == null || finish.estimateWeight == null) return '-'
  const difference = finish.actualWeight - finish.estimateWeight
  const className = difference > 0 ? 'is-positive' : difference < 0 ? 'is-negative' : ''
  return <span className={className}>{signedKg(difference)}</span>
}

function signedKg(value: number) {
  if (value === 0) return formatKg(0)
  return `${value > 0 ? '+' : '-'}${formatKg(Math.abs(value))}`
}

function renderSummary(totals: ReturnType<typeof calculateFinishedProductTotals>) {
  return (
    <Table.Summary.Row className="finished-products-summary">
      <Table.Summary.Cell index={0}>内部合计</Table.Summary.Cell>
      <Table.Summary.Cell index={1}>可交付 {totals.deliverableCount} 件</Table.Summary.Cell>
      <Table.Summary.Cell index={2}>已回录 {totals.recordedCount} 件</Table.Summary.Cell>
      <Table.Summary.Cell index={3} />
      <Table.Summary.Cell index={4} align="right">{formatKg(totals.estimateWeight)}</Table.Summary.Cell>
      <Table.Summary.Cell index={5} align="right">{formatKg(totals.actualWeight)}</Table.Summary.Cell>
      <Table.Summary.Cell index={6} align="right">{signedKg(totals.difference)}</Table.Summary.Cell>
    </Table.Summary.Row>
  )
}
