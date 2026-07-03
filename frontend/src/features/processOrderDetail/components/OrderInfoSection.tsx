import type { ReactNode } from 'react'
import { Button, Empty, Tag } from 'antd'
import { EditOutlined, PushpinOutlined } from '@ant-design/icons'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import { IS_INVOICE, ORDER_STATUS, PRIORITY } from '../../../constants/processOrder'
import { dict } from '../../../components/processOrder/shared/detailHelpers'
import { buildDetailMetrics, formatMoney, formatTon } from '../orderDetailUtils'

interface Props {
  canEditRemark?: boolean
  detail?: ProcessOrderDetailVO
  onEditRemark?: () => void
}

interface InfoItemProps {
  label: string
  value?: ReactNode
}

export default function OrderInfoSection({ canEditRemark, detail, onEditRemark }: Props) {
  const order = detail?.order
  const metrics = buildDetailMetrics(detail)
  const status = order?.orderStatus != null ? ORDER_STATUS[order.orderStatus] : undefined

  return (
    <section className="order-detail-section">
      <div className="order-detail-section__header">
        <h2 className="order-detail-section__title">单据信息</h2>
        {canEditRemark && (
          <Button type="link" size="small" icon={<EditOutlined />} onClick={onEditRemark}>
            编辑备注
          </Button>
        )}
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
          <InfoItem label="预估成品重量" value={formatTon(metrics.totalEstimateWeight)} />
          <InfoItem label="实际成品重量" value={metrics.totalActualWeight > 0 ? formatTon(metrics.totalActualWeight) : '-'} />
          <InfoItem label="加工费" value={formatMoney(order?.totalProcessAmount)} />
          <InfoItem label="附加费" value={formatMoney(order?.totalExtraAmount)} />
          <InfoItem label="总金额" value={formatMoney(order?.totalAmount)} />
        </div>
        <OrderRemarkNotice
          remark={order?.remark}
          remarkLong={order?.remarkLong}
          canEditRemark={canEditRemark}
          onEditRemark={onEditRemark}
        />
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

function OrderRemarkNotice({
  canEditRemark,
  onEditRemark,
  remark,
  remarkLong,
}: {
  canEditRemark?: boolean
  onEditRemark?: () => void
  remark?: string
  remarkLong?: string
}) {
  const hasRemark = Boolean(remark || remarkLong)

  return (
    <div className={`order-detail-remark ${hasRemark ? 'order-detail-remark--active' : ''}`}>
      <div className="order-detail-remark__head">
        <span>
          <PushpinOutlined />
          生产备注
        </span>
        {canEditRemark && (
          <Button type="link" size="small" icon={<EditOutlined />} onClick={onEditRemark}>
            编辑备注
          </Button>
        )}
      </div>
      {hasRemark ? (
        <div className="order-detail-remark__content">
          {remark && <p>{remark}</p>}
          {remarkLong && <p>{remarkLong}</p>}
        </div>
      ) : (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无生产备注" />
      )}
    </div>
  )
}
