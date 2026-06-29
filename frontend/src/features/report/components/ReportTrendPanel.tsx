import type { MonthlyReportVO } from '../../../types/report'
import { formatMoney, formatTon } from '../utils/reportFormatters'

interface Props {
  monthly: MonthlyReportVO[]
}

export default function ReportTrendPanel({ monthly }: Props) {
  const maxTon = Math.max(...monthly.map((item) => item.totalTon ?? 0), 0)

  return (
    <section className="report-panel report-panel--trend">
      <div className="report-panel__head">
        <div>
          <h3>月度趋势</h3>
          <p>按完成加工单的制单月份聚合。</p>
        </div>
      </div>
      <div className="report-trend">
        {monthly.length === 0 ? (
          <div className="report-empty mes-empty">当前周期暂无完成加工单</div>
        ) : (
          monthly.map((item) => (
            <div className="report-trend__item" key={item.month}>
              <div className="report-trend__bar">
                <i style={{ height: `${maxTon > 0 ? Math.max(8, ((item.totalTon ?? 0) / maxTon) * 100) : 0}%` }} />
              </div>
              <strong>{item.month}</strong>
              <span>{formatTon(item.totalTon)}</span>
              <em>{formatMoney(item.totalAmount)}</em>
            </div>
          ))
        )}
      </div>
    </section>
  )
}
