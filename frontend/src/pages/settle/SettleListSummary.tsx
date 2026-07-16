import type { SettleListSummary as Summary } from '../../types/settle'
import { formatMoney } from '../../features/settle/utils/settleFormatters'
import '../DocumentListSummary.css'

export default function SettleListSummary({ summary }: { summary?: Summary }) {
  return (
    <div className="document-list-summary" aria-label="结算汇总">
      <Metric label="有效应收" value={formatMoney(summary?.activeTotalAmount)} />
      <Metric label="累计已收" value={formatMoney(summary?.activeReceivedAmount)} />
      <Metric label="当前未收" value={formatMoney(summary?.activeUnreceivedAmount)} danger />
      <Metric label="优惠结清" value={formatMoney(summary?.activeDiscountAmount)} />
    </div>
  )
}

function Metric({ danger, label, value }: { danger?: boolean; label: string; value: string }) {
  return <div data-danger={danger || undefined}><span>{label}</span><strong>{value}</strong></div>
}
