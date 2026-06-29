import { Button, Empty, Tag } from 'antd'
import { ORDER_STATUS, PRIORITY } from '../../../constants/processOrder'
import type { DashboardRecentOrder } from '../../../types/dashboard'
import { formatKg } from '../../report/utils/reportFormatters'
import DashboardPanelHead from './DashboardPanelHead'

interface Props {
  loading: boolean
  onOpenOrder: (uuid: string) => void
  onOpenOrders: () => void
  orders: DashboardRecentOrder[]
}

export default function DashboardOrderList({ loading, onOpenOrder, onOpenOrders, orders }: Props) {
  return (
    <section className="dashboard-panel dashboard-orders">
      <DashboardPanelHead
        action={<Button onClick={onOpenOrders} size="small">全部单据</Button>}
        subtitle="追踪配置、下发、加工和回录进度。"
        title="最近加工单"
      />
      <div className="dashboard-orders__list">
        {orders.length === 0 && !loading ? (
          <Empty description="暂无加工单" image={Empty.PRESENTED_IMAGE_SIMPLE} />
        ) : (
          orders.map((order) => (
            <button className="dashboard-order-row" key={order.uuid} onClick={() => order.uuid && onOpenOrder(order.uuid)}>
              <div className="dashboard-order-row__main">
                <strong>{order.orderNo ?? '-'}</strong>
                <span>{order.customerName ?? '未设置客户'} · {order.orderDate ?? '-'}</span>
                <em>原卷 {formatKg(order.originalWeight)} / 成品 {formatKg(order.finishWeight)}</em>
              </div>
              <div className="dashboard-order-row__meta">
                <Tag color={ORDER_STATUS[order.orderStatus ?? 0]?.color}>{ORDER_STATUS[order.orderStatus ?? 0]?.text}</Tag>
                <Tag color={priorityColor(order.priority)}>{PRIORITY[order.priority ?? 1] ?? '普通'}</Tag>
                <Tag color={order.printStatus === 1 ? 'blue' : 'default'}>{order.printStatus === 1 ? '已打印' : '未打印'}</Tag>
              </div>
            </button>
          ))
        )}
      </div>
    </section>
  )
}

function priorityColor(priority?: number) {
  if (priority === 3) return 'red'
  if (priority === 2) return 'orange'
  return 'default'
}
