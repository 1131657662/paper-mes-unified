import type { ReactNode } from 'react'
import { Button } from 'antd'
import { EditOutlined, PushpinOutlined } from '@ant-design/icons'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import { buildBillingInfo } from './orderBillingInfo'

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

  return (
    <section className="order-detail-section">
      <div className="order-detail-section__header">
        <h2 className="order-detail-section__title">结算与备注</h2>
      </div>
      <div className="order-detail-section__body">
        <div className="order-detail-info-grid">
          {buildBillingInfo(order).map((item) => <InfoItem key={item.label} {...item} />)}
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
  if (!hasRemark) {
    return (
      <div className="order-detail-remark order-detail-remark--empty">
        <RemarkTitle />
        <span className="order-detail-remark__empty">暂无生产备注</span>
        <RemarkEditButton enabled={canEditRemark} onEdit={onEditRemark} />
      </div>
    )
  }

  return (
    <div className="order-detail-remark order-detail-remark--active">
      <div className="order-detail-remark__head">
        <RemarkTitle />
        <RemarkEditButton enabled={canEditRemark} onEdit={onEditRemark} />
      </div>
      <div className="order-detail-remark__content">
        {remark && <p>{remark}</p>}
        {remarkLong && <p>{remarkLong}</p>}
      </div>
    </div>
  )
}

function RemarkTitle() {
  return <span><PushpinOutlined />生产备注</span>
}

function RemarkEditButton({ enabled, onEdit }: { enabled?: boolean; onEdit?: () => void }) {
  if (!enabled) return null
  return <Button type="link" size="small" icon={<EditOutlined />} onClick={onEdit}>编辑备注</Button>
}
