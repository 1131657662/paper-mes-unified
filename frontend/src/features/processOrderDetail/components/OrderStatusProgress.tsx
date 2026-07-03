import { Steps } from 'antd'
import type { ProcessOrder } from '../../../types/processOrder'

const items = [
  { title: '草稿' },
  { title: '待下发' },
  { title: '加工中' },
  { title: '待回录' },
  { title: '已完成' },
  { title: '已结算' },
]

interface Props {
  order?: ProcessOrder
}

export default function OrderStatusProgress({ order }: Props) {
  const isVoided = order?.orderStatus === 6
  const stepItems = isVoided ? [...items, { title: '已作废' }] : items
  const current = isVoided ? stepItems.length - 1 : Math.max(0, Math.min(order?.orderStatus ?? 0, items.length - 1))

  return (
    <div className="order-detail-progress">
      <Steps size="small" current={current} status={isVoided ? 'error' : undefined} items={stepItems} />
    </div>
  )
}
