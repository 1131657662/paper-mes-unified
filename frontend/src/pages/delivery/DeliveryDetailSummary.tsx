import { StatisticCard } from '@ant-design/pro-components'
import { Card, Descriptions, Tag } from 'antd'
import { DELIVERY_STATUS, SETTLE_BLOCK_ACTION } from '../../constants/delivery'
import { formatTon } from '../../features/delivery/utils/deliveryFormatters'
import type { DeliveryOrder } from '../../types/delivery'
import { formatDateTime } from '../../utils/dateTime'
import { deliverySignLabel, deliveryStockLabel, resolveDeliveryOverview } from './deliveryDetailState'

export function DeliveryStatusTag({ status }: { status?: number }) {
  const item = status ? DELIVERY_STATUS[status] : undefined
  return item ? <Tag className="mes-status-tag" color={item.color}>{item.text}</Tag> : <>-</>
}

export function DeliveryOverview({ order }: { order: DeliveryOrder }) {
  return (
    <StatisticCard.Group className="document-amount-overview" gutter={[12, 12]} ghost>
      {overviewItems(order).map((item) => (
        <StatisticCard
          className={`document-amount-card ${item.tone ? `document-amount-card--${item.tone}` : ''}`}
          colSpan={{ xs: 24, md: 12, xl: 6 }}
          key={item.label}
          statistic={{ description: item.hint, title: item.label, value: item.value }}
        />
      ))}
    </StatisticCard.Group>
  )
}

export function DeliveryPickupInfo({ order }: { order: DeliveryOrder }) {
  return (
    <Card className="document-module-card" title="提货与签收">
      <Descriptions bordered size="small" column={3}>
        <Descriptions.Item label="货主">{order.customerName}</Descriptions.Item>
        <Descriptions.Item label="收货客户">{order.receiverCustomerName || ''}</Descriptions.Item>
        <Descriptions.Item label="提货人">{order.pickerName || '-'}</Descriptions.Item>
        <Descriptions.Item label="车牌号">{order.carNo || '-'}</Descriptions.Item>
        <Descriptions.Item label="柜号">{order.containerNo || '-'}</Descriptions.Item>
        <Descriptions.Item label="签收人">{order.signUser || '-'}</Descriptions.Item>
        <Descriptions.Item label="签收时间">{formatDateTime(order.signTime)}</Descriptions.Item>
        <Descriptions.Item label="备注" span="filled">{order.remark || '-'}</Descriptions.Item>
      </Descriptions>
    </Card>
  )
}

function overviewItems(order: DeliveryOrder): DeliveryOverviewItem[] {
  const state = resolveDeliveryOverview(order)
  return [
    { label: '出库卷数', tone: 'primary', value: `${order.totalCount ?? 0} 卷` },
    { label: '出库重量', value: formatTon(order.totalWeight) },
    {
      hint: signHint(order, state.signState),
      label: '签收状态',
      tone: state.signState === 'SIGNED' ? 'success' : state.signState === 'PENDING' ? 'warning' : undefined,
      value: deliverySignLabel(state.signState),
    },
    {
      hint: stockHint(order, state.stockState),
      label: '实物状态',
      value: deliveryStockLabel(state.stockState),
    },
  ]
}

function signHint(order: DeliveryOrder, state: ReturnType<typeof resolveDeliveryOverview>['signState']): string {
  if (state === 'NOT_REQUIRED') return '单据已作废，不需要司机签收'
  if (order.signTime) return formatDateTime(order.signTime)
  return '司机签收后扣减库存'
}

function stockHint(order: DeliveryOrder, state: ReturnType<typeof resolveDeliveryOverview>['stockState']): string {
  if (state === 'RELEASED') return '作废后已释放全部库存占用'
  if (state === 'DEDUCTED') return '已签收并扣减成品库存'
  if (!order.settleBlockAction) return '无结算拦截'
  return SETTLE_BLOCK_ACTION[order.settleBlockAction] ?? '结算拦截状态未知'
}

interface DeliveryOverviewItem {
  hint?: string
  label: string
  tone?: 'primary' | 'success' | 'warning'
  value: string
}
