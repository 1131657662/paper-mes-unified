import type { ProcessOrder } from '../../../types/processOrder'
import { IS_INVOICE, ORDER_SETTLE_TYPE } from '../../../constants/processOrder'
import { dict } from '../../../components/processOrder/shared/detailHelpers'
import { formatMoney } from '../orderDetailUtils'

export interface BillingInfoItem {
  label: string
  value: string
}

export function buildBillingInfo(order?: ProcessOrder): BillingInfoItem[] {
  const fees = [
    { label: '加急费', amount: order?.urgentFee },
    { label: '托盘费', amount: order?.palletFee },
    { label: '装卸费', amount: order?.loadingFee },
    { label: '运费', amount: order?.freightFee },
    { label: '其他费用', amount: order?.otherFee },
  ].filter((item) => Number(item.amount ?? 0) > 0)

  return [
    { label: '结算方式', value: settleText(order) },
    { label: '开票', value: dict(IS_INVOICE, order?.isInvoice) },
    { label: '税率', value: order?.taxRate == null ? '-' : `${order.taxRate}%` },
    ...fees.map((item) => ({ label: item.label, value: formatMoney(item.amount) })),
    { label: '附加费合计', value: formatMoney(order?.totalExtraAmount) },
  ]
}

function settleText(order?: ProcessOrder): string {
  const mode = dict(ORDER_SETTLE_TYPE, order?.settleType)
  if (order?.settleType !== 2 || order.settleDay == null) return mode
  return `${mode} ${order.settleDay}日`
}
