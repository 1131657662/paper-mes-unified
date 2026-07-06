import { formatMoney } from '../utils/settleFormatters'

interface Props {
  amount: number
  count: number
  label?: string
}

export default function SettleSelectedSummary({ amount, count, label = '已选' }: Props) {
  return (
    <div className="document-module-summary">
      <span>{label} <strong>{count}</strong> 单</span>
      <span>预计应收 <strong>{formatMoney(amount)}</strong></span>
    </div>
  )
}
