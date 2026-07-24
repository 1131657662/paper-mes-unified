import type { Customer } from '../../types/customer'
import type { DefaultPlanOptions } from './draftMappers'

interface UuidRecord {
  uuid: string
}

export function toReferenceOptions<T extends UuidRecord>(items: T[], labelKey: keyof T) {
  return items.map((item) => ({ label: String(item[labelKey]), value: item.uuid }))
}

export function toCustomerOptions(items: Customer[]) {
  return items.map((item) => ({
    label: item.customerName,
    value: item.uuid,
    defaultInvoice: item.defaultInvoice,
    priceIncludeTax: item.priceIncludeTax,
    rewindPrice: item.rewindPrice,
    sawPrice: item.sawPrice,
    settleDay: item.settleDay,
    settleType: item.settleType,
    taxRate: item.taxRate,
  }))
}

export function createDefaultPlanOptions(
  customer: Customer | undefined,
  spareCount: number,
): DefaultPlanOptions {
  return {
    spareCount,
    sawPrice: customer?.sawPrice,
    rewindPrice: customer?.rewindPrice,
  }
}
