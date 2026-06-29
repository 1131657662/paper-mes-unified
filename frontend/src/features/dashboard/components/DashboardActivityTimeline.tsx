import { Empty, Tag, Timeline } from 'antd'
import { ORDER_STATUS } from '../../../constants/processOrder'
import type { DashboardRecentOrder } from '../../../types/dashboard'
import DashboardPanelHead from './DashboardPanelHead'

interface Props {
  orders: DashboardRecentOrder[]
}

export default function DashboardActivityTimeline({ orders }: Props) {
  return (
    <section className="dashboard-panel dashboard-activity">
      <DashboardPanelHead title="最新动态" subtitle="最近创建或更新的加工单状态。" />
      {orders.length === 0 ? (
        <Empty description="暂无动态" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      ) : (
        <Timeline
          className="dashboard-activity__timeline"
          items={orders.slice(0, 6).map((order) => ({
            color: timelineColor(order.orderStatus),
            children: <TimelineItem order={order} />,
          }))}
        />
      )}
    </section>
  )
}

function TimelineItem({ order }: { order: DashboardRecentOrder }) {
  const status = ORDER_STATUS[order.orderStatus ?? 0]
  return (
    <div className="dashboard-activity__item">
      <strong>{order.orderNo ?? '-'}</strong>
      <span>{order.customerName ?? '未设置客户'} · {order.orderDate ?? '-'}</span>
      <Tag color={status?.color}>{status?.text ?? '未知'}</Tag>
    </div>
  )
}

function timelineColor(status?: number) {
  if (status === 3) return 'orange'
  if (status === 4 || status === 5) return 'green'
  if (status === 2) return 'blue'
  return 'gray'
}
