import { Space, Tag } from 'antd'
import dayjs from 'dayjs'
import { SETTLE_STATUS } from '../../constants/settle'
import type { SettleOrder } from '../../types/settle'

export default function SettleDetailHeader({ order }: { order: SettleOrder }) {
  return <Space size={6} wrap>
    <SettleStatusTag status={order.settleStatus} />
    {isOverdue(order) && <Tag color="error">已逾期</Tag>}
  </Space>
}

function SettleStatusTag({ status }: { status?: number }) {
  const item = status ? SETTLE_STATUS[status] : undefined
  return item ? <Tag className="mes-status-tag" color={item.color}>{item.text}</Tag> : <>-</>
}

function isOverdue(order: SettleOrder) {
  return order.settleStatus !== 3 && order.settleStatus !== 4
    && Number(order.unreceivedAmount ?? 0) > 0
    && Boolean(order.periodEnd)
    && dayjs(order.periodEnd).isBefore(dayjs(), 'day')
}
