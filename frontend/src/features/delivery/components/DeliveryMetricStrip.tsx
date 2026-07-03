import type { AvailableFinishVO, DeliveryOrder } from '../../../types/delivery'
import { availableFinishWeight, formatTon } from '../utils/deliveryFormatters'

interface Props {
  orders: DeliveryOrder[]
  selectedFinishes: AvailableFinishVO[]
  stock: AvailableFinishVO[]
}

export default function DeliveryMetricStrip({ orders, selectedFinishes, stock }: Props) {
  const stockWeight = stock.reduce((sum, item) => sum + availableFinishWeight(item), 0)
  const selectedWeight = selectedFinishes.reduce((sum, item) => sum + availableFinishWeight(item), 0)
  const pendingOrders = orders.filter((item) => item.deliveryStatus === 1)
  const shippedOrders = orders.filter((item) => item.deliveryStatus === 2)

  return (
    <div className="delivery-metrics mes-metrics">
      <Metric title="可出库存" main={`${stock.length} 卷`} sub={formatTon(stockWeight)} />
      <Metric title="本次选择" main={`${selectedFinishes.length} 卷`} sub={formatTon(selectedWeight)} />
      <Metric title="待签收单" main={`${pendingOrders.length} 张`} sub={`${pendingCount(pendingOrders)} 卷`} />
      <Metric title="已出库单" main={`${shippedOrders.length} 张`} sub={`${pendingCount(shippedOrders)} 卷`} />
    </div>
  )
}

function Metric({ main, sub, title }: { title: string; main: string; sub: string }) {
  return (
    <div className="delivery-metric mes-metric">
      <span>{title}</span>
      <strong>{main}</strong>
      <em>{sub}</em>
    </div>
  )
}

function pendingCount(orders: DeliveryOrder[]) {
  return orders.reduce((sum, item) => sum + (item.totalCount ?? 0), 0)
}
