import { Empty } from 'antd'
import type { ReportDimensionVO } from '../../../types/report'
import { formatPercent, formatTonFromKg } from '../utils/reportFormatters'

interface Props {
  rows: ReportDimensionVO[]
  title: string
}

export default function ReportTopicTrend({ rows, title }: Props) {
  const visible = rows.slice(-12)
  const maximum = Math.max(...visible.map((row) => Number(row.originalWeight || 0)), 1)
  return (
    <section className="report-topic-panel report-topic-trend">
      <header>
        <div><h3>{title}</h3><p>对比投入、产出与损耗率，最多展示最近 12 个周期。</p></div>
        <div className="report-topic-trend__legend" aria-label="趋势图例">
          <span><i className="report-topic-trend__input" />投入</span>
          <span><i className="report-topic-trend__output" />产出</span>
        </div>
      </header>
      {visible.length === 0 ? <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} /> : (
        <div className="report-topic-trend__rows">
          {visible.map((row) => <TrendRow key={row.dimensionKey} row={row} maximum={maximum} />)}
        </div>
      )}
    </section>
  )
}

function TrendRow({ maximum, row }: { maximum: number; row: ReportDimensionVO }) {
  const inputWidth = Math.max((Number(row.originalWeight || 0) / maximum) * 100, 1)
  const outputWidth = Math.max((Number(row.finishWeight || 0) / maximum) * 100, 1)
  return (
    <div className="report-topic-trend__row">
      <strong>{row.dimensionName}</strong>
      <div className="report-topic-trend__bars"
        aria-label={`投入 ${formatTonFromKg(row.originalWeight)}，产出 ${formatTonFromKg(row.finishWeight)}`}>
        <span className="report-topic-trend__input" style={{ width: `${inputWidth}%` }} />
        <span className="report-topic-trend__output" style={{ width: `${outputWidth}%` }} />
      </div>
      <span>{formatTonFromKg(row.finishWeight)}</span>
      <em>{formatPercent(row.lossRatio)}</em>
    </div>
  )
}
