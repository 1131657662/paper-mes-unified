import { Tag } from 'antd'
import { formatMoney } from '../../features/settle/utils/settleFormatters'
import type { SettleQuoteVO } from '../../types/settle'

interface Props {
  error?: boolean
  quote?: SettleQuoteVO
  loading: boolean
}

export default function SettleQuoteSummary({ error, loading, quote }: Props) {
  if (loading) return <span>正在试算...</span>
  if (error) return <Tag color="error">试算失败，暂不能生成结算单</Tag>
  if (!quote) return <span>选择加工单后显示准确试算</span>
  return (
    <div className="settle-quote-summary">
      <Tag color={quote.isInvoice === 1 ? 'blue' : 'default'}>
        {quote.isInvoice === 1 ? '开票' : '不开票'}
      </Tag>
      <span>未税 {formatMoney(quote.amountNoTax)}</span>
      <span>税费 {formatMoney(quote.taxAmount)}</span>
      <strong>应收 {formatMoney(quote.totalAmount)}</strong>
      {quote.pendingPriceCount > 0 && <Tag color="warning">{quote.pendingPriceCount} 单待核价</Tag>}
    </div>
  )
}
