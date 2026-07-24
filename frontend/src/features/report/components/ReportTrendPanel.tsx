import { FallOutlined, MinusOutlined, RiseOutlined } from '@ant-design/icons'
import Empty from 'antd/es/empty'
import { lazy, Suspense } from 'react'
import type { ReportDimensionVO } from '../../../types/report'
import { formatMoney, formatTonFromKg } from '../utils/reportFormatters'
import { fillMonthlySeries } from '../utils/reportMonthlySeries'

const ReportTrendChart = lazy(() => import('./ReportTrendChart'))

interface Props {
  dateFrom?: string
  dateTo?: string
  monthly: ReportDimensionVO[]
}

export default function ReportTrendPanel({ dateFrom, dateTo, monthly }: Props) {
  const points = fillMonthlySeries(monthly, { dateFrom, dateTo })

  return (
    <section className="report-panel report-panel--trend">
      <div className="report-panel__head report-trend-head">
        <div>
          <h3>月度加工应收趋势</h3>
          <p>按归属月份观察应收与加工吨位变化。</p>
        </div>
        {points.length > 0 && <TrendSummary points={points} />}
      </div>
      {points.length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="当前条件暂无月度数据" />
      ) : (
        <div className="report-curve">
          <div className="report-curve__chart">
            <Suspense fallback={<div className="report-curve__loading" />}>
              <ReportTrendChart monthly={points} />
            </Suspense>
          </div>
          <div className="report-curve__axis">
            {points.map((item) => (
              <div key={item.dimensionKey}>
                <strong>{item.dimensionName}</strong>
                <span>{formatMoney(item.totalAmount)}</span>
                <em>{formatTonFromKg(item.originalWeight)}</em>
              </div>
            ))}
          </div>
        </div>
      )}
    </section>
  )
}

function TrendSummary({ points }: { points: ReportDimensionVO[] }) {
  const current = points.at(-1)
  const previous = points.at(-2)
  const change = amountChange(current?.totalAmount, previous?.totalAmount)
  const currentPeriod = current?.dimensionName ?? '当前期间'
  const comparisonPeriod = previous?.dimensionName ? `较 ${previous.dimensionName}` : '无上期数据'
  return (
    <div className="report-trend-summary" aria-label="月度趋势摘要">
      <TrendValue label="本期应收" period={currentPeriod} value={formatMoney(current?.totalAmount)} />
      <TrendValue label="环比" period={comparisonPeriod} value={change.text} tone={change.tone} />
      <TrendValue label="本期吨位" period={currentPeriod} value={formatTonFromKg(current?.originalWeight)} />
    </div>
  )
}

interface TrendValueProps {
  label: string
  period: string
  tone?: TrendTone
  value: string
}

function TrendValue({ label, period, tone, value }: TrendValueProps) {
  return (
    <div className={`report-trend-value report-trend-value--${tone ?? 'neutral'}`}>
      <span className="report-trend-value__label">{label}</span>
      <strong className="report-trend-value__main">{toneIcon(tone)}{value}</strong>
      <small className="report-trend-value__period">{period}</small>
    </div>
  )
}

type TrendTone = 'down' | 'neutral' | 'up'

function amountChange(currentValue?: number, previousValue?: number): { text: string; tone: TrendTone } {
  const current = Number(currentValue ?? 0)
  const previous = Number(previousValue ?? 0)
  if (previous === 0) {
    return { text: current === 0 ? '持平 0.0%' : '新增', tone: current === 0 ? 'neutral' : 'up' }
  }
  const percent = ((current - previous) / Math.abs(previous)) * 100
  if (percent === 0) return { text: '持平 0.0%', tone: 'neutral' }
  return {
    text: `${percent > 0 ? '上升' : '下降'} ${Math.abs(percent).toFixed(1)}%`,
    tone: percent > 0 ? 'up' : 'down',
  }
}

function toneIcon(tone?: TrendTone) {
  if (tone === 'up') return <RiseOutlined aria-hidden />
  if (tone === 'down') return <FallOutlined aria-hidden />
  if (tone === 'neutral') return <MinusOutlined aria-hidden />
  return null
}
