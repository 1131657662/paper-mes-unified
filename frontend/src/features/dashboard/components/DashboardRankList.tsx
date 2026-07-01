import { Empty, Segmented } from 'antd'
import { useState } from 'react'
import type { DashboardRank } from '../../../types/dashboard'
import { formatMoney, formatTonFromKg } from '../../report/utils/reportFormatters'
import DashboardPanelHead from './DashboardPanelHead'

interface Props {
  emptyText: string
  mode: 'amount' | 'weight'
  items: DashboardRank[]
  yearlyItems?: DashboardRank[]
  yearlyEmptyText?: string
  subtitle?: string
  title: string
}

export default function DashboardRankList({ emptyText, items, mode, subtitle, title, yearlyEmptyText, yearlyItems }: Props) {
  const [range, setRange] = useState<RankRange>('month')
  const displayItems = range === 'year' && yearlyItems ? yearlyItems : items
  const displayEmptyText = range === 'year' && yearlyItems ? yearlyEmptyText ?? emptyText : emptyText
  const max = Math.max(...displayItems.map((item) => rankValue(item, mode)), 0)
  const canSwitch = !!yearlyItems

  return (
    <section className="dashboard-panel dashboard-rank">
      <DashboardPanelHead
        extra={canSwitch && (
          <Segmented
            className="dashboard-rank__range"
            options={[
              { label: '本月', value: 'month' },
              { label: '本年', value: 'year' },
            ]}
            size="small"
            value={range}
            onChange={(value) => setRange(value as RankRange)}
          />
        )}
        subtitle={subtitle}
        title={title}
      />
      <div className="dashboard-rank__list">
        {displayItems.length === 0 ? (
          <Empty description={displayEmptyText} image={Empty.PRESENTED_IMAGE_SIMPLE} />
        ) : (
          displayItems.map((item, index) => (
            <div className="dashboard-rank-row" key={item.id ?? item.name}>
              <span className={`dashboard-rank-row__index dashboard-rank-row__index--${index < 3 ? 'hot' : 'normal'}`}>{index + 1}</span>
              <div className="dashboard-rank-row__label">
                <strong>{item.name ?? '-'}</strong>
                <span>{item.count ?? 0} 单 / {formatTonFromKg(item.weight)}</span>
                <div className="dashboard-rank-row__track">
                  <i style={{ width: `${barWidth(rankValue(item, mode), max)}%` }} />
                </div>
              </div>
              <b>{mode === 'amount' ? formatMoney(item.amount) : formatTonFromKg(item.weight)}</b>
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

type RankRange = 'month' | 'year'
