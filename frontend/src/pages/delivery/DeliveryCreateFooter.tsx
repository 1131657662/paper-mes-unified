import { CheckOutlined, ProfileOutlined } from '@ant-design/icons'
import { Button, Tag } from 'antd'
import { formatTon } from '../../features/delivery/utils/deliveryFormatters'
import type { DeliverySelectionSummary } from './deliverySelectionModel'

interface Props {
  disabled: boolean
  loading: boolean
  summary: DeliverySelectionSummary
  onCancel: () => void
  onReview: () => void
  onSubmit: () => void
}

export default function DeliveryCreateFooter(props: Props) {
  const { summary } = props
  return (
    <footer className="delivery-create-footer">
      <div className="delivery-create-footer__summary">
        <Button
          className="delivery-create-footer__review"
          type="text"
          size="small"
          icon={<ProfileOutlined />}
          disabled={summary.totalCount === 0}
          onClick={props.onReview}
        >
          本次已选 <strong>{summary.totalCount}</strong> 卷
        </Button>
        <span>提货合计 <strong>{formatTon(summary.totalWeight)}</strong></span>
        <span className="delivery-create-footer__kind">
          成品 <strong>{summary.productCount}</strong> 卷 / {formatTon(summary.productWeight)}
        </span>
        <span className="delivery-create-footer__kind">
          余料 <strong>{summary.remainCount}</strong> 卷 / {formatTon(summary.remainWeight)}
        </span>
        {summary.riskCount > 0 && <Tag color="warning">{summary.riskCount} 卷需要放行确认</Tag>}
      </div>
      <div className="delivery-create-footer__actions">
        <Button onClick={props.onCancel}>取消</Button>
        <Button
          type="primary"
          icon={<CheckOutlined />}
          loading={props.loading}
          disabled={summary.totalCount === 0 || props.disabled}
          onClick={props.onSubmit}
        >
          生成待出库单
        </Button>
      </div>
    </footer>
  )
}
