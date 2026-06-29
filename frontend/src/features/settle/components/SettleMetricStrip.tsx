import type { SettleCandidateVO, SettleOrder } from '../../../types/settle'
import { formatMoney } from '../utils/settleFormatters'

interface Props {
  orders: SettleOrder[]
  selectedCandidates: SettleCandidateVO[]
}

export default function SettleMetricStrip({ orders, selectedCandidates }: Props) {
  const totalReceivable = orders.reduce((sum, item) => sum + (item.totalAmount ?? 0), 0)
  const totalUnreceived = orders.reduce((sum, item) => sum + (item.unreceivedAmount ?? 0), 0)
  const selectedAmount = selectedCandidates.reduce((sum, item) => sum + (item.totalAmount ?? 0), 0)
  const pendingReceives = orders.filter((item) => item.settleStatus !== 3)

  return (
    <div className="settle-metrics mes-metrics">
      <Metric title="本次选择" main={`${selectedCandidates.length} 单`} sub={formatMoney(selectedAmount)} />
      <Metric title="结算单" main={`${orders.length} 张`} sub={formatMoney(totalReceivable)} />
      <Metric title="待收款" main={`${pendingReceives.length} 张`} sub={formatMoney(totalUnreceived)} />
      <Metric title="已结清" main={`${orders.length - pendingReceives.length} 张`} sub="收款完成" />
    </div>
  )
}

function Metric({ main, sub, title }: { title: string; main: string; sub: string }) {
  return (
    <div className="settle-metric mes-metric">
      <span>{title}</span>
      <strong>{main}</strong>
      <em>{sub}</em>
    </div>
  )
}
