import { Button, Tag, Tooltip, message } from 'antd'
import { CopyOutlined, DownloadOutlined } from '@ant-design/icons'
import MesPageHeader from '../../../components/layout/MesPageHeader'
import MesTooltip from '../../../components/biz/MesTooltip'
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
  const printText = order?.printStatus === 1
    ? `已打印 ${order.printCount ?? 1} 次`
    : order?.orderStatus != null && order.orderStatus >= 2 && order.orderStatus < 6
      ? '已下发，未打印'
      : '未打印'

  const copyOrderNo = async () => {
    if (!order?.orderNo) return
    try {
      await navigator.clipboard.writeText(order.orderNo)
      message.success('加工单号已复制')
    } catch {
      message.error('复制失败，请手动复制')
    }
  }

  return (
    <MesPageHeader
      actions={(
        <MesTooltip title={!order ? '加工单详情加载完成后才能导出' : undefined}>
          <span className="order-detail-action-slot">
            <Button
              aria-label={!order ? '后台导出加工单资料：加工单详情未加载' : '后台导出加工单资料'}
              icon={<DownloadOutlined />}
              loading={exporting}
              disabled={!order}
              onClick={onExport}
            >
              后台导出
            </Button>
          </span>
        </MesTooltip>
      )}
      className="order-detail-hero"
      onBack={onBack}
      tags={(
        <>
          {status && <Tag color={status.color}>{status.text}</Tag>}
          {order?.isMixProcess === 1 && <Tag color="purple">混合工艺</Tag>}
          <Tag color={order?.printStatus === 1 ? 'green' : order?.orderStatus != null && order.orderStatus >= 2 && order.orderStatus < 6 ? 'orange' : 'default'}>{printText}</Tag>
        </>
      )}
      title={order?.orderNo ?? '加工单详情'}
      titleExtra={order?.orderNo && (
        <Tooltip title="复制加工单号">
          <Button
            aria-label="复制加工单号"
            className="order-detail-copy-no"
            icon={<CopyOutlined />}
            size="small"
            type="text"
            onClick={copyOrderNo}
          />
        </Tooltip>
      )}
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
