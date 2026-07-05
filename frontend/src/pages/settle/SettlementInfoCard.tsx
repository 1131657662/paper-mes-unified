import { Card, Descriptions } from 'antd'
import { INVOICE_TYPE, SETTLE_TYPE } from '../../constants/settle'
import { formatMoney } from '../../features/settle/utils/settleFormatters'
import type { SettleDetailVO } from '../../types/settle'

export default function SettlementInfoCard({ detail }: { detail: SettleDetailVO }) {
  return (
    <Card className="document-module-card" title="结算基础信息">
      <Descriptions bordered size="small" column={{ xs: 1, md: 2, xl: 3 }}>
        <Descriptions.Item label="结算单号">{detail.order.settleNo}</Descriptions.Item>
        <Descriptions.Item label="客户">{detail.order.customerName}</Descriptions.Item>
        <Descriptions.Item label="结算类型">{SETTLE_TYPE[detail.order.settleType] || '-'}</Descriptions.Item>
        <Descriptions.Item label="结算日期">{detail.order.settleDate}</Descriptions.Item>
        <Descriptions.Item label="账期">{detail.order.periodStart || '-'} ~ {detail.order.periodEnd || '-'}</Descriptions.Item>
        <Descriptions.Item label="是否开票">{INVOICE_TYPE[detail.order.isInvoice] || '-'}</Descriptions.Item>
        <Descriptions.Item label="未税金额">{formatMoney(detail.order.amountNoTax)}</Descriptions.Item>
        <Descriptions.Item label="税点加价">{formatMoney(detail.order.taxAmount)}</Descriptions.Item>
        <Descriptions.Item label="已结清">{formatMoney(detail.order.receivedAmount)}</Descriptions.Item>
        <Descriptions.Item label="现金实收">{formatMoney(detail.order.cashReceivedAmount)}</Descriptions.Item>
        <Descriptions.Item label="废纸抵扣">{formatMoney(detail.order.scrapOffsetAmount)}</Descriptions.Item>
        <Descriptions.Item label="未收金额">{formatMoney(detail.order.unreceivedAmount)}</Descriptions.Item>
        <Descriptions.Item label="备注" span="filled">{detail.order.remark || '-'}</Descriptions.Item>
      </Descriptions>
    </Card>
  )
}
