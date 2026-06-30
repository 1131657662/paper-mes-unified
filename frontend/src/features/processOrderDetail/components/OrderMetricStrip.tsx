import { StatisticCard } from '@ant-design/pro-components'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import {
  buildDetailMetrics,
  formatKg,
  formatMoney,
  formatTon,
} from '../orderDetailUtils'

interface Props {
  detail?: ProcessOrderDetailVO
}

export default function OrderMetricStrip({ detail }: Props) {
  const metrics = buildDetailMetrics(detail)
  const finishWeight = metrics.totalActualWeight || metrics.totalEstimateWeight

  const items = [
    {
      label: '母卷',
      value: `${metrics.rollCount} 卷`,
      hint: formatKg(metrics.totalOriginalWeight),
    },
    {
      label: '成品',
      value: `${metrics.finishCount} 件`,
      hint: metrics.spareCount > 0 ? `备用 ${metrics.spareCount} 件` : formatTon(finishWeight),
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
    <StatisticCard.Group className="order-detail-metrics" gutter={[10, 10]} ghost>
      {items.map((item) => (
        <StatisticCard
          className="order-detail-metric"
          colSpan={{ xs: 24, sm: 12, lg: 6 }}
          key={item.label}
          statistic={{
            description: item.hint,
            title: item.label,
            value: item.value,
          }}
        />
      ))}
    </StatisticCard.Group>
  )
}
