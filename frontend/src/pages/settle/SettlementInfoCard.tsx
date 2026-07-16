import { Card, Descriptions } from 'antd'
import { INVOICE_TYPE, SETTLE_TYPE } from '../../constants/settle'
import { formatMoney } from '../../features/settle/utils/settleFormatters'
import type { SettleDetailVO } from '../../types/settle'

export default function SettlementInfoCard({ detail }: { detail: SettleDetailVO }) {
  return (
    <Card className="document-module-card" title="结算与开票">
      <Descriptions bordered size="small" column={3}>
        <Descriptions.Item label="结算类型">{SETTLE_TYPE[detail.order.settleType] || '-'}</Descriptions.Item>
        <Descriptions.Item label="账期">{detail.order.periodStart || '-'} ~ {detail.order.periodEnd || '-'}</Descriptions.Item>
        <Descriptions.Item label="是否开票">{INVOICE_TYPE[detail.order.isInvoice] || '-'}</Descriptions.Item>
        <Descriptions.Item label="未税金额">{formatMoney(detail.order.amountNoTax)}</Descriptions.Item>
        <Descriptions.Item label="税点加价">{formatMoney(detail.order.taxAmount)}</Descriptions.Item>
        <Descriptions.Item label="优惠核销">{formatMoney(detail.order.discountAmount)}</Descriptions.Item>
        <Descriptions.Item label="备注" span="filled">{detail.order.remark || '-'}</Descriptions.Item>
      </Descriptions>
    </Card>
  )
}
