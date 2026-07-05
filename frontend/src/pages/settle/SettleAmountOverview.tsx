import { StatisticCard } from '@ant-design/pro-components'
import { formatMoney } from '../../features/settle/utils/settleFormatters'
import type { SettleOrder } from '../../types/settle'

interface Props {
  order: SettleOrder
}

export default function SettleAmountOverview({ order }: Props) {
  const items: SettleOverviewItem[] = [
    { label: '应收总额', tone: 'primary', value: formatMoney(order.totalAmount) },
    { label: '已结清金额', tone: 'success', value: formatMoney(order.receivedAmount) },
    { label: '现金实收', value: formatMoney(order.cashReceivedAmount) },
    { label: '废纸抵扣', value: formatMoney(order.scrapOffsetAmount) },
    { label: '未收金额', tone: 'warning', value: formatMoney(order.unreceivedAmount) },
    {
      hint: '锯纸费 / 复卷费 / 额外费',
      label: '费用构成',
      value: `${formatMoney(order.sawAmount)} / ${formatMoney(order.rewindAmount)} / ${formatMoney(order.extraAmount)}`,
    },
  ]

  return (
    <StatisticCard.Group className="document-amount-overview" gutter={[12, 12]} ghost>
      {items.map((item) => (
        <StatisticCard
          className={`document-amount-card ${item.tone ? `document-amount-card--${item.tone}` : ''}`}
          colSpan={{ xs: 24, md: 12, xl: 4 }}
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

interface SettleOverviewItem {
  hint?: string
  label: string
  tone?: 'primary' | 'success' | 'warning'
  value: string
}
