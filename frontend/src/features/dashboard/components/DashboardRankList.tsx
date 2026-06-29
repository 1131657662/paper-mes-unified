import { Empty } from 'antd'
import type { DashboardRank } from '../../../types/dashboard'
import { formatKg, formatMoney } from '../../report/utils/reportFormatters'
import DashboardPanelHead from './DashboardPanelHead'

interface Props {
  emptyText: string
  mode: 'amount' | 'weight'
  items: DashboardRank[]
  subtitle?: string
  title: string
}

export default function DashboardRankList({ emptyText, items, mode, subtitle, title }: Props) {
  const max = Math.max(...items.map((item) => rankValue(item, mode)), 0)

  return (
    <section className="dashboard-panel dashboard-rank">
      <DashboardPanelHead subtitle={subtitle} title={title} />
      <div className="dashboard-rank__list">
        {items.length === 0 ? (
          <Empty description={emptyText} image={Empty.PRESENTED_IMAGE_SIMPLE} />
        ) : (
          items.map((item, index) => (
            <div className="dashboard-rank-row" key={item.id ?? item.name}>
              <span className={`dashboard-rank-row__index dashboard-rank-row__index--${index < 3 ? 'hot' : 'normal'}`}>{index + 1}</span>
              <div className="dashboard-rank-row__label">
                <strong>{item.name ?? '-'}</strong>
                <span>{item.count ?? 0} {mode === 'amount' ? '单' : '卷'} / {formatKg(item.weight)}</span>
              </div>
              <div className="dashboard-rank-row__track">
                <i style={{ width: `${barWidth(rankValue(item, mode), max)}%` }} />
              </div>
              <b>{mode === 'amount' ? formatMoney(item.amount) : formatKg(item.weight)}</b>
            </div>
          ))
        )}
      </div>
    </section>
  )
}

function rankValue(item: DashboardRank, mode: 'amount' | 'weight') {
  return mode === 'amount' ? Number(item.amount ?? 0) : Number(item.weight ?? 0)
}

function barWidth(value: number, max: number) {
  if (max <= 0) return 0
  return Math.max(8, (value / max) * 100)
}
