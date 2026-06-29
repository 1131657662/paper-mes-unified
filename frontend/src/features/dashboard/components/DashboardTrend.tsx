import dayjs from 'dayjs'
import type { DashboardTrend as DashboardTrendItem } from '../../../types/dashboard'
import { formatKg, formatMoney } from '../../report/utils/reportFormatters'
import DashboardPanelHead from './DashboardPanelHead'

interface Props {
  monthly: DashboardTrendItem[]
}

export default function DashboardTrend({ monthly }: Props) {
  const displayMonthly = fillRecentMonths(monthly)
  const maxWeight = Math.max(...displayMonthly.map((item) => item.originalWeight ?? 0), 0)
  const maxAmount = Math.max(...displayMonthly.map((item) => item.amount ?? 0), 0)

  return (
    <section className="dashboard-panel dashboard-trend">
      <DashboardPanelHead title="分析概览" subtitle="近六月已完成加工单的原卷重量、成品重量和加工费。" />
      <div className="dashboard-trend__chart">
        {displayMonthly.length === 0 ? (
          <div className="dashboard-empty">暂无完成数据</div>
        ) : (
          displayMonthly.map((item) => (
            <div className="dashboard-trend__item" key={item.month}>
              <div className="dashboard-trend__bar">
                <i className="dashboard-trend__bar-original" style={{ height: `${barHeight(item.originalWeight, maxWeight)}%` }} />
                <i className="dashboard-trend__bar-finish" style={{ height: `${barHeight(item.finishWeight, maxWeight)}%` }} />
              </div>
              <strong>{shortMonth(item.month)}</strong>
              <span>原卷 {formatKg(item.originalWeight)}</span>
              <em>{formatMoney(item.amount)}</em>
            </div>
          ))
        )}
      </div>
      <div className="dashboard-trend__footer">
        <Legend color="blue" label="原卷重量" />
        <Legend color="green" label="成品重量" />
        <span>最高加工费 {formatMoney(maxAmount)}</span>
      </div>
    </section>
  )
}

function barHeight(value?: number, max = 0) {
  if (max <= 0) return 0
  return Math.max(10, (Number(value ?? 0) / max) * 100)
}

function shortMonth(month?: string) {
  return month?.slice(5) ?? '-'
}

function fillRecentMonths(monthly: DashboardTrendItem[]) {
  const byMonth = new Map(monthly.map((item) => [item.month, item]))
  return Array.from({ length: 6 }, (_, index) => {
    const month = dayjs().subtract(5 - index, 'month').format('YYYY-MM')
    return byMonth.get(month) ?? { month, amount: 0, finishWeight: 0, orderCount: 0, originalWeight: 0 }
  })
}

function Legend({ color, label }: LegendProps) {
  return (
    <span className="dashboard-trend__legend">
      <i className={`dashboard-trend__legend-dot dashboard-trend__legend-dot--${color}`} />
      {label}
    </span>
  )
}

interface LegendProps {
  color: 'blue' | 'green'
  label: string
}
