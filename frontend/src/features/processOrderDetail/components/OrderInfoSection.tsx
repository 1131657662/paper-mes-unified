import type { ReactNode } from 'react'
import { Tag } from 'antd'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import { IS_INVOICE, ORDER_STATUS, PRIORITY } from '../../../constants/processOrder'
import { dict } from '../../../components/processOrder/shared/detailHelpers'
import { buildDetailMetrics, formatMoney, formatTon } from '../orderDetailUtils'

interface Props {
  detail?: ProcessOrderDetailVO
}

interface InfoItemProps {
  label: string
  value?: ReactNode
}

export default function OrderInfoSection({ detail }: Props) {
  const order = detail?.order
  const metrics = buildDetailMetrics(detail)
  const status = order?.orderStatus != null ? ORDER_STATUS[order.orderStatus] : undefined

  return (
    <section className="order-detail-section">
      <div className="order-detail-section__header">
        <h2 className="order-detail-section__title">单据信息</h2>
      </div>
      <div className="order-detail-section__body">
        <div className="order-detail-info-grid">
          <InfoItem label="加工单号" value={order?.orderNo} />
          <InfoItem label="客户" value={order?.customerName} />
          <InfoItem label="制单日期" value={order?.orderDate} />
          <InfoItem label="期望完成" value={order?.expectFinishDate} />
          <InfoItem label="优先级" value={dict(PRIORITY, order?.priority)} />
          <InfoItem label="状态" value={status ? <Tag color={status.color}>{status.text}</Tag> : '-'} />
          <InfoItem label="开票" value={dict(IS_INVOICE, order?.isInvoice)} />
          <InfoItem label="打印" value={order?.printStatus === 1 ? `已打印 ${order.printCount ?? 1} 次` : '未打印'} />
          <InfoItem label="原纸合计" value={formatTon(metrics.totalOriginalWeight)} />
          <InfoItem label="加工费" value={formatMoney(order?.totalProcessAmount)} />
          <InfoItem label="附加费" value={formatMoney(order?.totalExtraAmount)} />
          <InfoItem label="总金额" value={formatMoney(order?.totalAmount)} />
          <InfoItem label="备注" value={order?.remark || order?.remarkLong || '-'} />
        </div>
      </div>
    </section>
  )
}

function InfoItem({ label, value }: InfoItemProps) {
  return (
    <div className="order-detail-info-item">
      <div className="order-detail-info-item__label">{label}</div>
      <div className="order-detail-info-item__value">{value ?? '-'}</div>
    </div>
  )
}
