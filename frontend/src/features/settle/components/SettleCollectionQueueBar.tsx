import type { SettleCollectionQueue, SettleCollectionSummary } from '../../../types/settle'
import { formatMoney } from '../utils/settleFormatters'

interface Props {
  active: SettleCollectionQueue
  summary?: SettleCollectionSummary
  onChange: (value: SettleCollectionQueue) => void
}

const queueItems: Array<{
  key: SettleCollectionQueue
  label: string
  count: keyof SettleCollectionSummary
  amount: keyof SettleCollectionSummary
}> = [
  { key: 'overdue', label: '已逾期', count: 'overdueCount', amount: 'overdueAmount' },
  { key: 'today', label: '今日待收', count: 'dueTodayCount', amount: 'dueTodayAmount' },
  { key: 'upcoming', label: '后续到期', count: 'upcomingCount', amount: 'upcomingAmount' },
  { key: 'reminded', label: '今日已提醒', count: 'remindedTodayCount', amount: 'remindedTodayAmount' },
]

export default function SettleCollectionQueueBar({ active, summary, onChange }: Props) {
  return (
    <section className="settle-collection-queue" aria-label="催收队列">
      {queueItems.map((item) => (
        <button key={item.key} type="button" aria-pressed={active === item.key}
          className={`settle-collection-queue__item${active === item.key ? ' is-active' : ''}`}
          onClick={() => onChange(item.key)}>
          <span>{item.label}</span>
          <strong>{summary ? Number(summary[item.count]) : '-'}</strong>
          <em>{summary ? formatMoney(Number(summary[item.amount])) : '-'}</em>
        </button>
      ))}
    </section>
  )
}
