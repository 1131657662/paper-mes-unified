import { Tag, Typography } from 'antd'
import type { RollWeightBalance } from '../weightBalanceModel'
import { formatKg } from '../../../utils/numberFormatters'

interface Props {
  balance: RollWeightBalance
  compact?: boolean
}

const statusColor: Record<RollWeightBalance['status'], string> = {
  balanced: 'success',
  unbalanced: 'error',
  pending: 'warning',
  excluded: 'default',
}

export default function WeightBalanceStrip({ balance, compact = false }: Props) {
  if (balance.status === 'pending' || balance.status === 'excluded') {
    return (
      <div className="weight-balance-strip weight-balance-strip--message">
        <Tag color={statusColor[balance.status]}>{balance.label}</Tag>
        <Typography.Text type="secondary">{balance.detail}</Typography.Text>
      </div>
    )
  }

  return (
    <div className={`weight-balance-strip${compact ? ' weight-balance-strip--compact' : ''}`}>
      <div className="weight-balance-strip__heading">
        <Typography.Text strong>重量平衡</Typography.Text>
        <Tag color={statusColor[balance.status]}>{balance.label}</Tag>
      </div>
      <div className="weight-balance-strip__metrics">
        <Metric label="投入" value={balance.inputWeight} />
        <Metric label="成品" value={balance.finishWeight} />
        <Metric label={balance.outputWeightLabel} value={balance.trimWeight} />
        <Metric label="差值" value={balance.difference} danger={balance.blocking} />
      </div>
      <Typography.Text className="weight-balance-strip__detail" type={balance.blocking ? 'danger' : 'secondary'}>
        {balance.detail}
      </Typography.Text>
    </div>
  )
}

function Metric({ danger, label, value }: { danger?: boolean; label: string; value: number }) {
  return (
    <div className="weight-balance-strip__metric">
      <span>{label}</span>
      <strong className={danger ? 'weight-balance-strip__value--danger' : undefined}>{formatKg(value)}</strong>
    </div>
  )
}
