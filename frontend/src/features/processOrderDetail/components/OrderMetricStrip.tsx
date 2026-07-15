import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import {
  buildDetailMetrics,
  formatMoney,
  formatTon,
} from '../orderDetailUtils'

interface Props {
  detail?: ProcessOrderDetailVO
}

export default function OrderMetricStrip({ detail }: Props) {
  const metrics = buildDetailMetrics(detail)

  const items = [
    {
      label: '母卷',
      value: `${metrics.rollCount} 卷`,
      hint: formatTon(metrics.totalOriginalWeight),
    },
    {
      label: '成品',
      value: `${metrics.finishCount} 件`,
      hint: finishWeightHint(metrics),
    },
    {
      label: '加工',
      value: metrics.processLabel,
      hint: `${metrics.stepCount} 道工序 / ${metrics.knifeCount} 刀`,
    },
    {
      label: '费用',
      value: formatMoney(detail?.order.totalAmount),
      hint: `加工费 ${formatMoney(detail?.order.totalProcessAmount)}`,
    },
  ]

  return (
    <div className="order-execution__metrics">
      {items.map((item) => (
        <div className="order-execution-metric" key={item.label}>
          <span>{item.label}</span>
          <strong>{item.value}</strong>
          <small>{item.hint}</small>
        </div>
      ))}
    </div>
  )
}

function finishWeightHint(metrics: ReturnType<typeof buildDetailMetrics>) {
  const parts = [`预估 ${formatTon(metrics.totalEstimateWeight)}`]
  if (metrics.totalActualWeight > 0) parts.push(`实际 ${formatTon(metrics.totalActualWeight)}`)
  if (metrics.spareCount > 0) parts.push(`备用 ${metrics.spareCount} 件`)
  return parts.join(' / ')
}
