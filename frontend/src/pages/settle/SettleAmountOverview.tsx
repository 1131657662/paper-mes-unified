import { StatisticCard } from '@ant-design/pro-components'
import { formatMoney } from '../../features/settle/utils/settleFormatters'
import type { SettleOrder } from '../../types/settle'

interface Props {
  order: SettleOrder
}

export default function SettleAmountOverview({ order }: Props) {
  const items: SettleOverviewItem[] = [
    { label: '应收总额', tone: 'primary', value: formatMoney(order.totalAmount) },
    {
      hint: `现金 ${formatMoney(order.cashReceivedAmount)} / 废纸 ${formatMoney(order.scrapOffsetAmount)} / 优惠 ${formatMoney(order.discountAmount)}`,
      label: '已结清金额',
      tone: 'success',
      value: formatMoney(order.receivedAmount),
    },
    { label: '未收金额', tone: 'warning', value: formatMoney(order.unreceivedAmount) },
    {
      hint: `锯纸 ${formatMoney(order.sawAmount)} / 复卷 ${formatMoney(order.rewindAmount)} / 额外 ${formatMoney(order.extraAmount)}`,
      label: '费用构成',
      value: formatMoney(Number(order.sawAmount ?? 0) + Number(order.rewindAmount ?? 0) + Number(order.extraAmount ?? 0)),
    },
  ]

  return (
    <StatisticCard.Group className="document-amount-overview" gutter={[12, 12]} ghost>
      {items.map((item) => (
        <StatisticCard
          className={`document-amount-card ${item.tone ? `document-amount-card--${item.tone}` : ''}`}
          colSpan={{ xs: 24, md: 12, xl: 6 }}
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
