import { CheckCircleOutlined, ExclamationCircleOutlined } from '@ant-design/icons'
import { Tag, Typography } from 'antd'
import { formatKg, formatWholeKg } from '../../utils/numberFormatters'
import type { FinishCustomerRevisionPreview } from './customerSpecTypes'

export default function CustomerSpecPreviewBand({ data }: { data?: FinishCustomerRevisionPreview }) {
  if (!data) return <div className="customer-spec-preview-band is-empty"><Typography.Text type="secondary">尚未预览</Typography.Text></div>
  return (
    <div className={`customer-spec-preview-band${data.hasErrors ? ' has-errors' : ''}`}>
      <span className="customer-spec-preview-band__status">{data.hasErrors ? <ExclamationCircleOutlined /> : <CheckCircleOutlined />}<Typography.Text strong>{data.hasErrors ? '存在校验错误' : `V${data.nextRevisionNo} 可发布`}</Typography.Text></span>
      <span>已选 {data.itemCount} 件</span>
      <span>实物 {formatKg(data.physicalTotalWeight)}</span>
      <span>客户单据 {formatWholeKg(data.customerTotalWeight)}</span>
      <Tag color={data.differenceWeight === 0 ? 'default' : 'gold'}>差额 {formatWholeKg(data.differenceWeight)}</Tag>
    </div>
  )
}
