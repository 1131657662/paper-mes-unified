import { EditOutlined, HistoryOutlined, TagsOutlined } from '@ant-design/icons'
import { Button, Skeleton, Tag, Typography } from 'antd'
import { formatKg } from '../../utils/numberFormatters'
import { customerSpecificationLabel } from './customerSpecModel'
import type { FinishCustomerRevisionPreview } from './customerSpecTypes'
import './CustomerSpecification.css'

interface Props {
  canEdit: boolean
  data?: FinishCustomerRevisionPreview
  loading?: boolean
  onEdit: () => void
  onHistory: () => void
}

export default function CustomerRequirementStrip({ canEdit, data, loading, onEdit, onHistory }: Props) {
  if (loading) return <Skeleton active paragraph={{ rows: 1 }} title={false} />
  const changed = data?.items.filter((item) => item.specificationChanged || item.weightChanged) ?? []
  const labels = uniqueLabels(data)
  return (
    <div className={`customer-requirement-strip${changed.length ? ' is-overridden' : ''}`}>
      <div className="customer-requirement-strip__icon"><TagsOutlined /></div>
      <div className="customer-requirement-strip__content">
        <div className="customer-requirement-strip__title">
          <Typography.Text strong>客户标签口径</Typography.Text>
          <Tag color={changed.length ? 'blue' : 'default'}>V{Math.max(0, (data?.nextRevisionNo ?? 1) - 1)}</Tag>
          {changed.length > 0 && <Tag color="gold">{changed.length} 件已转换</Tag>}
        </div>
        <div className="customer-requirement-strip__specs">
          {labels.length ? labels.map((label) => <span key={label}>{label}</span>) : <span>暂无成品口径</span>}
        </div>
      </div>
      <div className="customer-requirement-strip__totals">
        <span>客户单据 {formatKg(data?.customerTotalWeight)}</span>
        <span>实物结算 {formatKg(data?.physicalTotalWeight)}</span>
      </div>
      <div className="customer-requirement-strip__actions">
        <Button icon={<HistoryOutlined />} onClick={onHistory}>版本</Button>
        {canEdit && <Button type="primary" icon={<EditOutlined />} onClick={onEdit}>批量维护</Button>}
      </div>
    </div>
  )
}

function uniqueLabels(data?: FinishCustomerRevisionPreview) {
  return [...new Set((data?.items ?? []).map(customerSpecificationLabel).filter(Boolean))].slice(0, 5)
}
