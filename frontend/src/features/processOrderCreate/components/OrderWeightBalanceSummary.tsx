import { Tag, Typography } from 'antd'
import { formatKg } from '../../../utils/numberFormatters'
import type { OrderWeightBalance } from '../weightBalanceModel'
import './PreviewWeightBalance.css'

interface Props {
  balance: OrderWeightBalance
}

export default function OrderWeightBalanceSummary({ balance }: Props) {
  const checkedCount = balance.balancedCount + balance.unbalancedCount
  const status = summaryStatus(balance, checkedCount)
  return (
    <section className="order-weight-balance" aria-label="订单重量平衡">
      <div className="order-weight-balance__heading">
        <div>
          <Typography.Text strong>订单重量平衡</Typography.Text>
          <Typography.Text type="secondary">
            已校验 {checkedCount} 卷
            {balance.pendingCount > 0 ? `，待预览 ${balance.pendingCount} 卷` : ''}
            {balance.excludedCount > 0 ? `，无需校验 ${balance.excludedCount} 卷` : ''}
          </Typography.Text>
        </div>
        <Tag color={status.color}>{status.label}</Tag>
      </div>
      <div className="order-weight-balance__metrics">
        <SummaryMetric label="投入总重" value={balance.inputWeight} />
        <SummaryMetric label="成品总重" value={balance.finishWeight} />
        <SummaryMetric label="切边总重" value={balance.trimWeight} />
        <SummaryMetric label="未分配差值" value={balance.difference} danger={balance.blocking} />
      </div>
    </section>
  )
}

function summaryStatus(balance: OrderWeightBalance, checkedCount: number) {
  if (balance.blocking) return { color: 'error', label: `${balance.unbalancedCount} 卷未平衡` }
  if (balance.pendingCount > 0) return { color: 'warning', label: '等待全部预览' }
  if (checkedCount === 0) return { color: 'default', label: '无需开单校验' }
  return { color: 'success', label: '重量已平衡' }
}

function SummaryMetric({ danger, label, value }: { danger?: boolean; label: string; value: number }) {
  return (
    <div className="order-weight-balance__metric">
      <span>{label}</span>
      <strong className={danger ? 'order-weight-balance__value--danger' : undefined}>{formatKg(value)}</strong>
    </div>
  )
}
