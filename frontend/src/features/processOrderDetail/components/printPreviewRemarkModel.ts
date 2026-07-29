import type { ProcessOrderDetailVO } from '../../../types/processOrder'

export function orderRemark(detail: ProcessOrderDetailVO): string {
  return [detail.order.remark, detail.order.remarkLong]
    .map((item) => item?.trim())
    .filter(Boolean)
    .join('；')
}
