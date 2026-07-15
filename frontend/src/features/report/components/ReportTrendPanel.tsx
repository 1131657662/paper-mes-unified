import Empty from 'antd/es/empty'
import MesTooltip from '../../../components/biz/MesTooltip'
import type { ReportDimensionVO } from '../../../types/report'
import { formatMoney, formatTonFromKg } from '../utils/reportFormatters'

interface Props {
  monthly: ReportDimensionVO[]
}

export default function ReportTrendPanel({ monthly }: Props) {
  const points = monthly.map((item) => ({
    amount: Number(item.totalAmount ?? 0),
    label: item.dimensionName,
    weight: Number(item.originalWeight ?? 0),
  }))
  const max = Math.max(...points.map((item) => item.amount), 0)
  const path = buildPath(points.map((item) => item.amount), max)
  const dots = buildDots(points.map((item) => item.amount), max)

  return (
    <section className="report-panel report-panel--trend">
      <div className="report-panel__head">
        <div>
          <h3>月度加工应收曲线</h3>
          <p>按制单月份汇总加工应收，辅助查看淡旺季、产能与回款压力。</p>
        </div>
      </div>
      {points.length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="当前条件暂无月度数据" />
      ) : (
        <div className="report-curve">
          <div className="report-curve__chart">
            <svg viewBox="0 0 100 42" preserveAspectRatio="none" aria-hidden>
              <defs>
                <linearGradient id="reportCurveFill" x1="0" x2="0" y1="0" y2="1">
                  <stop offset="0%" stopColor="#1677ff" stopOpacity="0.24" />
                  <stop offset="100%" stopColor="#1677ff" stopOpacity="0.02" />
                </linearGradient>
              </defs>
              <path className="report-curve__area" d={`${path} L 100 42 L 0 42 Z`} />
              <path className="report-curve__line" d={path} />
              {dots.map((dot, index) => (
                <circle className="report-curve__dot" cx={dot.x} cy={dot.y} key={points[index]?.label} r="1.1" />
              ))}
            </svg>
            <div className="report-curve__hotspots">
              {dots.map((dot, index) => (
                <MesTooltip
                  key={points[index]?.label}
                  title={`${points[index]?.label ?? '-'}：应收 ${formatMoney(points[index]?.amount)} / 原纸 ${formatTonFromKg(points[index]?.weight)}`}
                >
                  <span
                    aria-label={`${points[index]?.label ?? '-'}月度加工应收`}
                    className="report-curve__hotspot"
                    style={{ left: `${dot.x}%`, top: `${(dot.y / 42) * 100}%` }}
                    tabIndex={0}
                  />
                </MesTooltip>
              ))}
            </div>
          </div>
          <div className="report-curve__axis">
            {points.map((item) => (
              <div key={item.label}>
                <strong>{item.label}</strong>
                <span>{formatMoney(item.amount)}</span>
                <em>{formatTonFromKg(item.weight)}</em>
              </div>
            ))}
          </div>
        </div>
      )}
    </section>
  )
}

function buildDots(values: number[], max: number) {
  const first = values[0]
  if (values.length === 1 && first != null) {
    return [{ x: 50, y: pointY(first, max) }]
  }
  return values.map((value, index) => ({
    x: (index / (values.length - 1)) * 100,
    y: pointY(value, max),
  }))
}

function buildPath(values: number[], max: number) {
  const first = values[0]
  if (values.length === 1 && first != null) {
    const y = pointY(first, max)
    return `M 0 ${y} L 100 ${y}`
  }
  return values
    .map((value, index) => {
      const x = (index / (values.length - 1)) * 100
      const y = pointY(value, max)
      return `${index === 0 ? 'M' : 'L'} ${x} ${y}`
    })
    .join(' ')
}

function pointY(value: number, max: number) {
  if (max <= 0) return 36
  return 36 - (value / max) * 28
}
