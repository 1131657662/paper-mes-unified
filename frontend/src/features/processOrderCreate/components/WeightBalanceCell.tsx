import { EditOutlined } from '@ant-design/icons'
import { Button, Space, Tag, Typography } from 'antd'
import { formatKg } from '../../../utils/numberFormatters'
import type { RollWeightBalance } from '../weightBalanceModel'
import './PreviewWeightBalance.css'

interface Props {
  balance: RollWeightBalance
  onEdit: () => void
}

const statusColor: Record<RollWeightBalance['status'], string> = {
  balanced: 'success',
  unbalanced: 'error',
  pending: 'warning',
  excluded: 'default',
}

export default function WeightBalanceCell({ balance, onEdit }: Props) {
  return (
    <Space direction="vertical" size={2} className="weight-balance-cell">
      <Space size={4} wrap>
        <Tag color={statusColor[balance.status]}>{balance.label}</Tag>
        {balance.blocking && (
          <Button type="link" size="small" icon={<EditOutlined />} onClick={onEdit}>
            返回修正
          </Button>
        )}
      </Space>
      <Typography.Text type={balance.blocking ? 'danger' : 'secondary'}>
        {balance.status === 'balanced' || balance.status === 'unbalanced'
          ? `投入 ${formatKg(balance.inputWeight)} / 差值 ${formatKg(balance.difference)}`
          : balance.detail}
      </Typography.Text>
    </Space>
  )
}
