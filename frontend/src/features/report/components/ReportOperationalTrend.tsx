import { Empty } from 'antd'
import type { ReportCollectionDimensionVO, ReportDeliveryDimensionVO,
  ReportInventoryDimensionVO, ReportOperationalAnalysisVO,
  ReportSettlementDimensionVO } from '../../../types/reportOperational'
import { formatMoney, formatTonFromKg } from '../utils/reportFormatters'

export default function ReportOperationalTrend({ analysis }: { analysis: ReportOperationalAnalysisVO }) {
  const rows = analysis.topicCode === 'inventory' ? analysis.stockInCohorts : analysis.monthlyTrend
  if (rows.length === 0) return <EmptyTrend />
  const maximum = Math.max(...rows.map((row) => trendValue(row)), 1)
  const legend = trendLegend[analysis.topicCode]
  return <section className="report-topic-panel report-topic-trend">
    <header><div><h3>{analysis.topicCode === 'inventory' ? '当前库存入库批次' : '月度趋势'}</h3>
      <p>{trendHint[analysis.topicCode]}</p></div>
      <div className="report-topic-trend__legend" aria-label="趋势图例">
        <span><i className="report-operational-trend__primary" />{legend.primary}</span>
        <span><i className="report-operational-trend__secondary" />{legend.secondary}</span>
      </div>
    </header>
    <div className="report-topic-trend__rows">
      {rows.slice(-12).map((row) => <div className="report-topic-trend__row" key={row.dimensionKey}>
        <strong>{row.dimensionName}</strong>
        <div className="report-topic-trend__bars" aria-label={trendRowLabel(analysis.topicCode, row, legend)}>
          <span className="report-operational-trend__primary"
            style={{ width: barWidth(trendValue(row), maximum) + '%' }} />
          <span className="report-operational-trend__secondary"
            style={{ width: barWidth(secondaryValue(row), maximum) + '%' }} />
        </div>
        <span>{displayValue(analysis.topicCode, row)}</span><em>{rowCount(row)}</em>
      </div>)}
    </div>
  </section>
}

function EmptyTrend() {
  return <section className="report-topic-panel"><header><div><h3>时间趋势</h3>
    <p>当前筛选范围暂无可展示数据</p></div></header><Empty image={Empty.PRESENTED_IMAGE_SIMPLE} /></section>
}

type TrendRow = ReportSettlementDimensionVO | ReportCollectionDimensionVO
  | ReportInventoryDimensionVO | ReportDeliveryDimensionVO

function trendValue(row: TrendRow): number {
  if ('totalAmount' in row) return Number(row.totalAmount || 0)
  if ('settledAmount' in row) return Number(row.settledAmount || 0)
  return Number(row.totalWeight || 0)
}

function secondaryValue(row: TrendRow): number {
  if ('receivedAmount' in row) return Number(row.receivedAmount || 0)
  if ('cashAmount' in row) return Number(row.cashAmount || 0)
  if ('completedWeight' in row) return Number(row.completedWeight || 0)
  return Number(row.lockedWeight || 0)
}

function displayValue(topic: ReportOperationalAnalysisVO['topicCode'], row: TrendRow) {
  if (topic === 'settlement') return formatMoney('totalAmount' in row ? row.totalAmount : 0)
  if (topic === 'collection') return formatMoney('settledAmount' in row ? row.settledAmount : 0)
  return formatTonFromKg('totalWeight' in row ? row.totalWeight : 0)
}

function rowCount(row: TrendRow) {
  if ('documentCount' in row) return row.documentCount + ' 单'
  if ('rollCount' in row) return row.rollCount + ' 卷'
  return row.recordCount + ' 笔'
}

function trendRowLabel(topic: ReportOperationalAnalysisVO['topicCode'], row: TrendRow,
  legend: { primary: string; secondary: string }) {
  const format = topic === 'settlement' || topic === 'collection' ? formatMoney : formatTonFromKg
  return `${legend.primary} ${format(trendValue(row))}，${legend.secondary} ${format(secondaryValue(row))}`
}

function barWidth(value: number, maximum: number) { return Math.max((value / maximum) * 100, value > 0 ? 1 : 0) }

const trendHint = {
  settlement: '金额按结算日期聚合，第二条线表示已收金额。',
  collection: '金额按到账时间聚合，第二条线表示现金到账。',
  inventory: '这是当前库存按首次入库月份分层，不是历史期末余额。',
  delivery: '重量按出库单日期聚合，第二条线表示已签收重量。',
} satisfies Record<ReportOperationalAnalysisVO['topicCode'], string>

const trendLegend = {
  settlement: { primary: '应收', secondary: '已收' },
  collection: { primary: '结清', secondary: '现金到账' },
  inventory: { primary: '库存重量', secondary: '锁定重量' },
  delivery: { primary: '计划重量', secondary: '已签收' },
} satisfies Record<ReportOperationalAnalysisVO['topicCode'], { primary: string; secondary: string }>
