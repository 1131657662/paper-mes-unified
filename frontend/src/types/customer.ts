import type { PageQuery } from './common'

/** 客户实体，与后端 Customer 对应（含 BaseEntity 通用字段，按需取用）。 */
export interface Customer {
  uuid: string
  customerCode?: string
  customerName: string
  contact?: string
  phone?: string
  /** 结算方式 1次结 2月结 */
  settleType?: number
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
  createTime?: string
  updateTime?: string
}

/** 客户新增/修改入参，与后端 CustomerSaveDTO 对应。 */
export interface CustomerSaveDTO {
  customerCode?: string
  customerName: string
  contact?: string
  phone?: string
  settleType?: number
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
}

/** 客户列表查询入参。 */
export interface CustomerQuery extends PageQuery {
  keyword?: string
}
