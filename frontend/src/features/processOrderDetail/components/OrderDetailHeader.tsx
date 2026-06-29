import { Space, Tag, Typography } from 'antd'
import MesPageHeader from '../../../components/layout/MesPageHeader'
import type { ProcessOrder } from '../../../types/processOrder'
import { IS_INVOICE, ORDER_STATUS, PRIORITY } from '../../../constants/processOrder'
import { dict } from '../../../components/processOrder/shared/detailHelpers'

const { Text } = Typography

interface Props {
  order?: ProcessOrder
  onBack?: () => void
}

export default function OrderDetailHeader({ order, onBack }: Props) {
  const status = order?.orderStatus != null ? ORDER_STATUS[order.orderStatus] : undefined

  return (
    <MesPageHeader
      actions={(
        <Space className="order-detail-hero__actions">
          <Text type="secondary">
            {order?.printStatus === 1 ? `已打印 ${order.printCount ?? 1} 次` : '未打印'}
          </Text>
        </Space>
      )}
      className="order-detail-hero"
      onBack={onBack}
      tags={(
        <>
          {status && <Tag color={status.color}>{status.text}</Tag>}
          {order?.isMixProcess === 1 && <Tag color="purple">混合工艺</Tag>}
        </>
      )}
      title={order?.orderNo ?? '加工单详情'}
      description={(
        <span className="order-detail-hero__meta">
          <span>客户：{order?.customerName ?? '-'}</span>
          <span>制单：{order?.orderDate ?? '-'}</span>
          <span>期望：{order?.expectFinishDate ?? '-'}</span>
          <span>优先级：{dict(PRIORITY, order?.priority)}</span>
          <span>开票：{dict(IS_INVOICE, order?.isInvoice)}</span>
        </span>
      )}
    />
  )
}
