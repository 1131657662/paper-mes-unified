import { ReloadOutlined } from '@ant-design/icons'
import { Button, Space, Tag } from 'antd'
import { formatMoney } from '../../features/settle/utils/settleFormatters'
import type { SettleQuoteVO } from '../../types/settle'

interface Props {
  emptyText: string
  error?: boolean
  quote?: SettleQuoteVO
  loading: boolean
  onRetry: () => void
}

export default function SettleQuoteSummary({ emptyText, error, loading, onRetry, quote }: Props) {
  if (loading) return <span>正在试算...</span>
  if (error) return (
    <Space size={4} wrap>
      <Tag color="error">试算失败，暂不能生成结算单</Tag>
      <Button type="link" size="small" icon={<ReloadOutlined />} onClick={onRetry}>重新试算</Button>
    </Space>
  )
  if (!quote) return <span>{emptyText}</span>
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
