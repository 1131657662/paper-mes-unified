import { SortAscendingOutlined } from '@ant-design/icons'
import { Tooltip, Typography } from 'antd'
import type { DeliveryCustomerSortField, DeliveryDetailSortField } from '../../types/deliverySort'
import type { DeliveryPrintProjection } from './deliveryPrintProjection'

interface Props {
  projection: DeliveryPrintProjection
}

const FIELD_LABELS = {
  orderNo: '加工单', finishRollNo: '卷号', paperName: '品名', gramWeight: '克重', spec: '规格',
  actualWeight: '件重', outWeight: '出库重量', remainingWeight: '剩余重量',
  originalSummary: '来源母卷', remark: '备注', actualRemark: '回录备注',
  customerPaperName: '客户品名', customerSpecification: '客户规格',
  customerDisplayWeight: '客户重量', customerRemark: '客户备注', sourceMotherRoll: '来源母卷',
} satisfies Record<DeliveryDetailSortField | DeliveryCustomerSortField, string>

export default function DeliveryPrintOrderSummary({ projection }: Props) {
  const items = projection.sortChain.map((item) => (
    `${FIELD_LABELS[item.field] ?? item.field} ${item.direction === 'asc' ? '↑' : '↓'}`
  ))
  const fullText = items.length ? items.join(' · ') : '原始单据顺序'
  const visibleText = items.length > 2
    ? `${items.slice(0, 2).join(' · ')} · 其余 ${items.length - 2} 项`
    : fullText
  return (
    <Tooltip title={fullText}>
      <span className="delivery-print-order-summary" aria-label={`当前顺序：${fullText}`}>
        <SortAscendingOutlined />
        <Typography.Text type="secondary">当前顺序：{visibleText}</Typography.Text>
      </span>
    </Tooltip>
  )
}
