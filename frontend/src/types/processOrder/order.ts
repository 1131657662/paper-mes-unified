import type { ProcessOrderSettlementMode } from '../settlementSemantics'

export type OrderSettlementMode = 'INHERIT' | 'OVERRIDE'

/** 加工单主表，与后端 ProcessOrder 对应（含 BaseEntity 通用字段，按需取用）。 */
export interface ProcessOrder {
  uuid: string
  version?: number
  orderNo?: string
  customerUuid?: string
  customerName?: string
  orderDate?: string
  expectFinishDate?: string
  /** 1普通 2加急 3特急 */
  priority?: number
  labelBrand?: string
  warehouseUuid?: string
  /** 历史班组字段，仅用于只读兼容；新请求不得写入。 */
  teamGroup?: string
  /** 1开票 2不开票 */
  isInvoice?: number
  /** 1次结 2月结，本单可覆盖客户默认值。 */
  settleType?: ProcessOrderSettlementMode
  settleDay?: number
  settleSource?: OrderSettlementMode
  settleCustomerVersion?: number
  settleOverrideReason?: string
  taxRate?: number
  urgentFee?: number
  palletFee?: number
  loadingFee?: number
  freightFee?: number
  otherFee?: number
  totalProcessAmount?: number
  totalExtraAmount?: number
  totalAmountNoTax?: number
  totalAmountTax?: number
  totalAmount?: number
  totalOriginalWeight?: number
  totalOriginalTon?: number
  totalFinishWeight?: number
  originalRollCount?: number
  originalPieceCount?: number
  originalRollWeight?: number
  finishRollCount?: number
  finishRollWeight?: number
  estimateFinishWeight?: number
  actualFinishWeight?: number
  spareRollCount?: number
  actualTotalKnife?: number
  /** 0草稿 1待下发 2加工中 3待回录 4已完成 5已结算 6已作废 */
  orderStatus?: number
  /** 0未确认打印，1已人工确认打印；不代表打印机设备回执。 */
  printStatus?: number
  printCount?: number
  /** 0单一工艺 1混合 */
  isMixProcess?: number
  processNames?: string[]
  remark?: string
  remarkLong?: string
  voidTime?: string
  voidUser?: string
  voidReason?: string
  createTime?: string
  updateTime?: string
}
