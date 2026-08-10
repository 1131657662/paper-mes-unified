import type { PageQuery } from './common'
import type { ProcessOrderSettlementMode } from './settlementSemantics'
import type {
  CustomerProcessPriceVO,
  CustomerProcessPriceVOBillingBasis,
  CustomerVO,
} from '../api/generated/customerReadOnly'

/** 客户实体，与后端 Customer 对应（含 BaseEntity 通用字段，按需取用）。 */
export type Customer = CustomerVO

export type CustomerProcessPriceBasis = CustomerProcessPriceVOBillingBasis

export type CustomerProcessPrice = CustomerProcessPriceVO

export interface CustomerProcessPriceSaveDTO {
  catalogUuid: string
  billingBasis: CustomerProcessPriceBasis
  price: number
  isDefault: number
}

/** 客户新增/修改入参，与后端 CustomerSaveDTO 对应。 */
export interface CustomerSaveDTO {
  customerCode?: string
  customerName: string
  contact?: string
  phone?: string
  settleType?: ProcessOrderSettlementMode
  settleDay?: number
  sawPrice?: number
  rewindPrice?: number
  defaultInvoice?: number
  priceIncludeTax?: number
  taxRate?: number
  taxNo?: string
  invoiceAddress?: string
  bankAccount?: string
  deliveryAddress?: string
  customerLevel?: number
  exportTemplate?: string
  remark?: string
  processPrices?: CustomerProcessPriceSaveDTO[]
}

/** 客户列表查询入参。 */
export interface CustomerQuery extends PageQuery {
  keyword?: string
}
