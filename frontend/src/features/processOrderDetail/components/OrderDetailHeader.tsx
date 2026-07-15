import { Button, Tag } from 'antd'
import { DownloadOutlined } from '@ant-design/icons'
import MesPageHeader from '../../../components/layout/MesPageHeader'
import type { ProcessOrder } from '../../../types/processOrder'
import { ORDER_STATUS, PRIORITY } from '../../../constants/processOrder'
import { dict } from '../../../components/processOrder/shared/detailHelpers'

interface Props {
  exporting?: boolean
  order?: ProcessOrder
  onBack?: () => void
  onExport?: () => void
}

export default function OrderDetailHeader({ exporting, order, onBack, onExport }: Props) {
  const status = order?.orderStatus != null ? ORDER_STATUS[order.orderStatus] : undefined
  const printText = order?.printStatus === 1 ? `已打印 ${order.printCount ?? 1} 次` : '未打印'

  return (
    <MesPageHeader
      actions={(
        <Button
          aria-label="导出加工单资料"
          icon={<DownloadOutlined />}
          loading={exporting}
          disabled={!order}
          onClick={onExport}
        >
          导出资料
        </Button>
      )}
      className="order-detail-hero"
      onBack={onBack}
      tags={(
        <>
          {status && <Tag color={status.color}>{status.text}</Tag>}
          {order?.isMixProcess === 1 && <Tag color="purple">混合工艺</Tag>}
          <Tag color={order?.printStatus === 1 ? 'green' : 'default'}>{printText}</Tag>
        </>
      )}
      title={order?.orderNo ?? '加工单详情'}
      description={(
        <span className="order-detail-hero__meta">
          <span>客户：{order?.customerName ?? '-'}</span>
          <span>制单：{order?.orderDate ?? '-'}</span>
          <span>期望：{order?.expectFinishDate ?? '-'}</span>
          <span>优先级：{dict(PRIORITY, order?.priority)}</span>
        </span>
      )}
    />
  )
}
