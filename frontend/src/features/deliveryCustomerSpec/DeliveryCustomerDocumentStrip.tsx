import { EditOutlined, FileSyncOutlined, HistoryOutlined } from '@ant-design/icons'
import { Button, Skeleton, Tag, Typography } from 'antd'
import { formatKg } from '../../utils/numberFormatters'
import type { DeliveryCustomerRevisionPreview } from './deliveryCustomerSpecTypes'
import './DeliveryCustomerSpec.css'

interface Props {
  canEdit: boolean
  data?: DeliveryCustomerRevisionPreview
  deliveryStatus?: number
  loading?: boolean
  onEdit: () => void
  onHistory: () => void
}

export default function DeliveryCustomerDocumentStrip({ canEdit, data, deliveryStatus, loading, onEdit, onHistory }: Props) {
  if (loading) return <Skeleton active paragraph={{ rows: 1 }} title={false} />
  const changed = data?.items.filter((item) => item.specificationChanged || item.weightChanged).length ?? 0
  const completed = deliveryStatus === 2
  const title = revisionTitle(data?.currentRevisionKind, completed)
  return (
    <div className={`delivery-customer-strip${changed ? ' is-corrected' : ''}`}>
      <div className="delivery-customer-strip__icon"><FileSyncOutlined /></div>
      <div className="delivery-customer-strip__main">
        <div className="delivery-customer-strip__title">
          <Typography.Text strong>{title}</Typography.Text>
          <Tag color={data?.currentRevisionNo ? 'blue' : 'default'}>V{data?.currentRevisionNo ?? 0}</Tag>
          {changed > 0 && <Tag color="gold">{changed} 件有调整</Tag>}
        </div>
        <span>{data?.itemCount ?? 0} 件 · 客户单据 {formatKg(data?.customerTotalWeight)} · 实物出库 {formatKg(data?.physicalTotalWeight)}</span>
      </div>
      <div className="delivery-customer-strip__actions">
        <Button icon={<HistoryOutlined />} onClick={onHistory}>版本记录</Button>
        {canEdit && <Button type="primary" icon={<EditOutlined />} onClick={onEdit}>{completed ? '创建客户更正版' : '维护客户口径'}</Button>}
      </div>
    </div>
  )
}

function revisionTitle(kind: DeliveryCustomerRevisionPreview['currentRevisionKind'] | undefined, completed: boolean) {
  if (kind === 'USER_REVISION') return '客户更正版'
  if (kind === 'SYSTEM_BASELINE') return '客户口径冻结基线'
  if (kind === 'HISTORICAL_BASELINE') return '历史出库实物基线'
  return completed ? '客户单据口径' : '待出库客户口径'
}
