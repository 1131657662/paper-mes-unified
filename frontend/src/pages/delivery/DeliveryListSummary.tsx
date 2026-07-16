import type { DeliveryListSummary as Summary } from '../../types/delivery'
import { formatTon } from '../../features/delivery/utils/deliveryFormatters'
import '../DocumentListSummary.css'

export default function DeliveryListSummary({ summary }: { summary?: Summary }) {
  return (
    <div className="document-list-summary" aria-label="出库汇总">
      <Metric label="有效卷数" value={`${summary?.activeRollCount ?? 0} 卷`} />
      <Metric label="有效重量" value={formatTon(summary?.activeWeight)} />
      <Metric label="待出库重量" value={formatTon(summary?.pendingWeight)} />
      <Metric label="已出库重量" value={formatTon(summary?.deliveredWeight)} />
    </div>
  )
}

function Metric({ label, value }: { label: string; value: string }) {
  return <div><span>{label}</span><strong>{value}</strong></div>
}
