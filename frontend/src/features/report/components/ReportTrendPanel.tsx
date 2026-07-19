import { lazy, Suspense } from 'react'
import Empty from 'antd/es/empty'
import type { ReportDimensionVO } from '../../../types/report'
import { formatMoney, formatTonFromKg } from '../utils/reportFormatters'

const ReportTrendChart = lazy(() => import('./ReportTrendChart'))

interface Props {
  monthly: ReportDimensionVO[]
}

export default function ReportTrendPanel({ monthly }: Props) {
  const points = monthly.map((item) => ({
    amount: Number(item.totalAmount ?? 0),
    label: item.dimensionName,
    weight: Number(item.originalWeight ?? 0),
  }))

  return (
    <section className="report-panel report-panel--trend">
      <div className="report-panel__head">
        <div>
          <h3>月度加工应收曲线</h3>
          <p>按归属月份汇总加工应收，辅助查看淡旺季、产能与回款压力。</p>
        </div>
      </div>
      {points.length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="当前条件暂无月度数据" />
      ) : (
        <div className="report-curve">
          <div className="report-curve__chart">
            <Suspense fallback={<div className="report-curve__loading" />}>
              <ReportTrendChart monthly={monthly} />
            </Suspense>
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
