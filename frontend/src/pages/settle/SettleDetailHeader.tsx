import { Space, Tag } from 'antd'
import { SETTLE_STATUS } from '../../constants/settle'
import { resolveSettleCollectionDisplay } from '../../features/settle/utils/settleCollectionStatus'
import type { SettleOrder } from '../../types/settle'

export default function SettleDetailHeader({ order }: { order: SettleOrder }) {
  return <Space size={6} wrap>
    <SettleStatusTag status={order.settleStatus} />
    {resolveSettleCollectionDisplay(order).overdue && <Tag color="error">已逾期</Tag>}
  </Space>
}

function SettleStatusTag({ status }: { status?: number }) {
  const item = status ? SETTLE_STATUS[status] : undefined
  return item ? <Tag className="mes-status-tag" color={item.color}>{item.text}</Tag> : <>-</>
}
