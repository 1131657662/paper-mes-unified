import { CheckCircleOutlined, ExclamationCircleOutlined } from '@ant-design/icons'
import { Tag, Typography } from 'antd'
import { formatKg, formatWholeKg } from '../../utils/numberFormatters'
import type { DeliveryCustomerRevisionPreview } from './deliveryCustomerSpecTypes'

export default function DeliveryCustomerPreviewBand({ data }: { data?: DeliveryCustomerRevisionPreview }) {
  if (!data) return <div className="delivery-customer-preview is-empty"><Typography.Text type="secondary">尚未预览</Typography.Text></div>
  return (
    <div className={`delivery-customer-preview${data.hasErrors ? ' has-errors' : ''}`}>
      <span className="delivery-customer-preview__status">{data.hasErrors ? <ExclamationCircleOutlined /> : <CheckCircleOutlined />}<Typography.Text strong>{data.hasErrors ? '存在校验错误' : `客户更正版 V${data.nextRevisionNo} 可发布`}</Typography.Text></span>
      <span>已选 {data.itemCount} 件</span>
      <span>实物 {formatKg(data.physicalTotalWeight)}</span>
      <span>客户单据 {formatWholeKg(data.customerTotalWeight)}</span>
      <Tag color={data.differenceWeight === 0 ? 'default' : 'gold'}>差额 {formatWholeKg(data.differenceWeight)}</Tag>
    </div>
  )
}
