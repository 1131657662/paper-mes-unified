import { formatKg } from '../../utils/numberFormatters'
import type { FinishRollStats } from './finishRollManagerModel'

interface Props {
  stats: FinishRollStats
}

export default function FinishRollSummary({ stats }: Props) {
  const items = [
    ['全部卷号', stats.total],
    ['有效卷号', stats.active],
    ['正式号', stats.official],
    ['备用号', stats.spare],
    ['已作废', stats.voided],
    ['实际重量', formatKg(stats.actualWeight)],
  ]
  return (
    <div className="finish-roll-summary">
      {items.map(([label, value]) => (
        <div className="finish-roll-summary__item" key={label}>
          <span>{label}</span>
          <strong>{value}</strong>
        </div>
      ))}
    </div>
  )
}
