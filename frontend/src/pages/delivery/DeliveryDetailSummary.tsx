import { Card, Descriptions, Tag } from 'antd'
import { StatisticCard } from '@ant-design/pro-components'
import { DELIVERY_STATUS, SETTLE_BLOCK_ACTION } from '../../constants/delivery'
import { formatTon } from '../../features/delivery/utils/deliveryFormatters'
import type { DeliveryOrder } from '../../types/delivery'
import { formatDateTime } from '../../utils/dateTime'

export function DeliveryStatusTag({ status }: { status?: number }) {
  const item = status ? DELIVERY_STATUS[status] : undefined
  return item ? <Tag className="mes-status-tag" color={item.color}>{item.text}</Tag> : <>-</>
}

export function DeliveryOverview({ order }: { order: DeliveryOrder }) {
  const items = overviewItems(order)
  return (
    <StatisticCard.Group className="document-amount-overview" gutter={[12, 12]} ghost>
      {items.map((item) => (
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
  return [
    { label: '出库卷数', tone: 'primary', value: `${order.totalCount ?? 0} 卷` },
    { label: '出库重量', value: formatTon(order.totalWeight) },
    {
      hint: order.signTime ? formatDateTime(order.signTime) : '司机签收后扣减库存',
      label: '签收状态',
      tone: order.deliveryStatus === 2 ? 'success' : 'warning',
      value: order.deliveryStatus === 2 ? '已签收' : '待签收',
    },
    {
      hint: order.deliveryStatus === 2 ? '客户显示可创建更正版，无需回退出库' : (order.settleBlockAction ? SETTLE_BLOCK_ACTION[order.settleBlockAction] : '无结算拦截'),
      label: '实物单状态',
      value: order.deliveryStatus === 1 ? '可调整卷与重量' : '实物已锁定',
    },
  ]
}

interface DeliveryOverviewItem {
  hint?: string
  label: string
  tone?: 'primary' | 'success' | 'warning'
  value: string
}
