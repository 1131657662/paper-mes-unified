import dayjs from 'dayjs'
import type { DashboardTrend as DashboardTrendItem } from '../../../types/dashboard'
import { formatMoney, formatNumber } from '../../report/utils/reportFormatters'
import DashboardPanelHead from './DashboardPanelHead'

interface Props {
  monthly: DashboardTrendItem[]
}

export default function DashboardTrend({ monthly }: Props) {
  const displayMonthly = fillRecentMonths(monthly)
  const maxAmount = Math.max(...displayMonthly.map((item) => Number(item.amount ?? 0)), 0)
  const totalAmount = displayMonthly.reduce((sum, item) => sum + Number(item.amount ?? 0), 0)
  const totalOrders = displayMonthly.reduce((sum, item) => sum + Number(item.orderCount ?? 0), 0)
  const points = buildPoints(displayMonthly, maxAmount)
  const hasReceivable = maxAmount > 0

  return (
    <section className="dashboard-panel dashboard-trend">
      <DashboardPanelHead title="分析概览" subtitle="近一年加工应收曲线，优先看月份波动和客户结算节奏。" />
      <div className="dashboard-trend__summary">
        <div>
          <span>年度加工应收</span>
          <strong>{formatMoney(totalAmount)}</strong>
        </div>
        <div>
          <span>完成加工单</span>
          <strong>{formatNumber(totalOrders)} 单</strong>
        </div>
        <div>
          <span>月均加工应收</span>
          <strong>{formatMoney(totalAmount / 12)}</strong>
        </div>
      </div>
      <div className="dashboard-trend__line-chart">
        <ReceivableLineChart hasReceivable={hasReceivable} points={points} />
        {!hasReceivable && (
          <div className="dashboard-trend__empty">
            <strong>暂无近一年加工应收</strong>
            <span>完成加工后，这里会自动形成月度应收曲线。</span>
          </div>
        )}
      </div>
      <div className="dashboard-trend__month-axis">
        {displayMonthly.map((item) => (
          <span key={item.month}>{shortMonth(item.month)}</span>
        ))}
      </div>
    </section>
  )
}

function ReceivableLineChart({ hasReceivable, points }: { hasReceivable: boolean; points: ChartPoint[] }) {
  const path = buildSmoothPath(points)
  const firstPoint = points[0]
  const lastPoint = points[points.length - 1]
  const areaPath = firstPoint && lastPoint ? `${path} L ${lastPoint.x} 196 L ${firstPoint.x} 196 Z` : ''

  return (
    <svg className="dashboard-trend__svg" viewBox="0 0 640 220" role="img" aria-label="近一年加工应收曲线">
      <defs>
        <linearGradient id="dashboard-income-fill" x1="0" x2="0" y1="0" y2="1">
          <stop offset="0%" stopColor="#1677ff" stopOpacity="0.22" />
          <stop offset="100%" stopColor="#1677ff" stopOpacity="0.02" />
        </linearGradient>
      </defs>
      {[0, 1, 2, 3].map((index) => (
        <line className="dashboard-trend__grid" key={index} x1="24" x2="616" y1={28 + index * 48} y2={28 + index * 48} />
      ))}
      <path className="dashboard-trend__area" d={areaPath} />
      <path className={hasReceivable ? 'dashboard-trend__line' : 'dashboard-trend__line dashboard-trend__line--empty'} d={path} />
      {points.map((point) => (
        <g key={point.month}>
          <circle className={hasReceivable ? 'dashboard-trend__dot' : 'dashboard-trend__dot dashboard-trend__dot--empty'} cx={point.x} cy={point.y} r="4.5" />
          {point.amount > 0 && (
            <text className="dashboard-trend__point-label" x={point.x} y={Math.max(18, point.y - 10)} textAnchor="middle">
              {shortMoney(point.amount)}
            </text>
          )}
        </g>
      ))}
    </svg>
  )
}

function buildSmoothPath(points: ChartPoint[]) {
  const first = points[0]
  if (!first) return ''
  if (points.length === 1) return `M ${first.x} ${first.y}`

  const segments = [`M ${first.x} ${first.y}`]
  for (let index = 0; index < points.length - 1; index += 1) {
    const previous = points[index - 1] ?? points[index]
    const current = points[index]
    const next = points[index + 1]
    const afterNext = points[index + 2] ?? next
    if (!previous || !current || !next || !afterNext) continue
    const controlOne = {
      x: current.x + (next.x - previous.x) / 6,
      y: current.y + (next.y - previous.y) / 6,
    }
    const controlTwo = {
      x: next.x - (afterNext.x - current.x) / 6,
      y: next.y - (afterNext.y - current.y) / 6,
    }
    segments.push(`C ${controlOne.x} ${controlOne.y}, ${controlTwo.x} ${controlTwo.y}, ${next.x} ${next.y}`)
  }
  return segments.join(' ')
}

function buildPoints(monthly: DashboardTrendItem[], maxAmount: number): ChartPoint[] {
  const chartWidth = 592
  const left = 24
  const top = 28
  const height = 168
  const step = chartWidth / Math.max(monthly.length - 1, 1)

  const divisor = Math.max(maxAmount, 1)
  return monthly.map((item, index) => {
    const amount = Number(item.amount ?? 0)
    return {
      amount,
      month: item.month ?? '',
      x: left + index * step,
      y: top + height - (amount / divisor) * height,
    }
  })
}

function fillRecentMonths(monthly: DashboardTrendItem[]) {
  const byMonth = new Map(monthly.map((item) => [item.month, item]))
  return Array.from({ length: 12 }, (_, index) => {
    const month = dayjs().subtract(11 - index, 'month').format('YYYY-MM')
    return byMonth.get(month) ?? { month, amount: 0, finishWeight: 0, orderCount: 0, originalWeight: 0 }
  })
}

function shortMonth(month?: string) {
  return month?.slice(5) ?? '-'
}

function shortMoney(value: number) {
  if (value >= 10000) return `${formatNumber(value / 10000, 1)}万`
  return formatNumber(value, 0)
}

interface ChartPoint {
  amount: number
  month: string
  x: number
  y: number
}
