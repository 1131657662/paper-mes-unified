import type { ReactNode } from 'react'
import { Button } from 'antd'
import { EditOutlined, PushpinOutlined } from '@ant-design/icons'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import { buildBillingInfo } from './orderBillingInfo'

interface Props {
  canEditPostProductionNote?: boolean
  canEditRemark?: boolean
  detail?: ProcessOrderDetailVO
  onEditPostProductionNote?: () => void
  onEditRemark?: () => void
}

interface InfoItemProps {
  label: string
  value?: ReactNode
}

export default function OrderInfoSection({ canEditPostProductionNote, canEditRemark, detail, onEditPostProductionNote, onEditRemark }: Props) {
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
        <PostProductionNoteNotice
          canEdit={canEditPostProductionNote}
          note={order?.postProductionNote}
          onEdit={onEditPostProductionNote}
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

function PostProductionNoteNotice({ canEdit, note, onEdit }: { canEdit?: boolean; note?: string; onEdit?: () => void }) {
  return (
    <div className={`order-detail-remark ${note ? 'order-detail-remark--active' : 'order-detail-remark--empty'}`}>
      <div className="order-detail-remark__head">
        <span><PushpinOutlined />后生产备注</span>
        <RemarkEditButton enabled={canEdit} onEdit={onEdit} />
      </div>
      {note ? <div className="order-detail-remark__content"><p>{note}</p></div>
        : <span className="order-detail-remark__empty">暂无后生产备注</span>}
    </div>
  )
}
