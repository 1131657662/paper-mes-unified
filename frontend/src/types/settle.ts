import type { PageQuery } from './common'
import type { OperationLog } from './operationLog'

export interface SettleOrder {
  uuid: string
  settleNo: string
  customerUuid: string
  customerName: string
  requestId?: string
  quoteVersion?: string
  quoteHash?: string
  settleType: number
  settleDate: string
  dueDate?: string
  periodStart?: string
  periodEnd?: string
  sawAmount: number
  rewindAmount: number
  extraAmount: number
  amountNoTax: number
  taxAmount: number
  totalAmount: number
  receivedAmount: number
  cashReceivedAmount?: number
  scrapOffsetAmount?: number
  discountAmount?: number
  discountReason?: string
  discountApprovalUuid?: string
  discountApprovedBy?: string
  unreceivedAmount: number
  reminderCount?: number
  lastReminderTime?: string
  lastReminderBy?: string
  lastReminderResult?: number
  nextFollowUpDate?: string
  isInvoice: number
  settleStatus: number
  voidReason?: string
  voidBy?: string
  voidTime?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface SettleDetail {
  uuid: string
  settleUuid: string
  orderUuid: string
  orderNo: string
  sawAmount: number
  rewindAmount: number
  standardProcessAmount?: number
  pricingAdjustmentAmount?: number
  pricingAdjustmentReason?: string
  extraAmount: number
  orderAmount: number
  remark?: string
}

export interface SettlePrintLine {
  settleUuid: string
  orderUuid: string
  orderNo: string
  orderDate?: string
  originalUuid: string
  originalLabel: string
  paperName?: string
  gramWeight?: number
  originalWidth?: number
  originalRollNo?: string
  originalExtraNo?: string
  actualGramWeight?: number
  actualWidth?: number
  originalDiameter?: number
  coreDiameter?: number
  originalLength?: number
  originalWeight?: number
  processMode?: number
  mainStepType?: number
  machineUuid?: string
  machineName?: string
  processText?: string
  processStepSummary?: string
  finishSummary?: string
  finishDetailSummary?: string
  finishCount?: number
  finishWeight?: number
  trimWeight?: number
  trimSummary?: string
  sawWeight?: number
  rewindWeight?: number
  sawUnitPrice?: number
  sawInvoiceUnitPrice?: number
  rewindUnitPrice?: number
  rewindInvoiceUnitPrice?: number
  sawAmount?: number
  rewindAmount?: number
  standardProcessAmount?: number
  pricingAdjustmentAmount?: number
  pricingAdjustmentReason?: string
  processAmount?: number
  extraAmount?: number
  extraFeeSummary?: string
  taxRate?: number
  taxAmount?: number
  lineAmount?: number
  isInvoice?: number
  remark?: string
  feeLines?: SettleFeeLine[]
}

export interface SettleFeeLine {
  feeType: 'saw' | 'rewind' | 'extra' | 'tax' | string
  feeName: string
  stageLevel?: number
  sourceText?: string
  outputText?: string
  quantity?: number
  quantityUnit?: string
  unitPrice?: number
  standardQuantity?: number
  standardAmount?: number
  billingMode?: number
  pricingAdjustmentAmount?: number
  pricingAdjustmentReason?: string
  amountNoTax?: number
  taxRate?: number
  taxAmount?: number
  amountTax?: number
  formulaText?: string
  remark?: string
}

export interface ReceiveRecord {
  uuid: string
  settleUuid: string
  requestId?: string
  receiveDate: string
  receiveAmount: number
  cashAmount?: number
  scrapOffsetAmount?: number
  discountAmount?: number
  scrapWeight?: number
  scrapUnitPrice?: number
  receiveType?: number
  payMethod?: number
  payNo?: string
  operator?: string
  recordStatus?: number
  cancelTime?: string
  cancelBy?: string
  cancelReason?: string
  remark?: string
}

export interface SettleDetailVO {
  order: SettleOrder
  details: SettleDetail[]
  receives: ReceiveRecord[]
  printLines?: SettlePrintLine[]
  operationLogs?: OperationLog[]
}

export interface SettleCandidateQuery {
  keyword?: string
  customerUuid?: string
  periodStart?: string
  periodEnd?: string
  current?: number
  size?: number
}

export interface SettleCandidateVO {
  orderUuid: string
  orderNo: string
  customerUuid: string
  customerName: string
  orderDate?: string
  accountingDate?: string
  settleType?: number
  settleDay?: number
  isInvoice?: number
  originalRollCount?: number
  originalRollWeight?: number
  finishRollCount?: number
  finishRollWeight?: number
  sawAmount?: number
  rewindAmount?: number
  standardProcessAmount?: number
  pricingAdjustmentAmount?: number
  extraAmount?: number
  totalAmount?: number
}

export interface SettleByOrderDTO {
  orderUuid: string
  settleDate?: string
  isInvoice?: number
  remark?: string
}

export interface SettleByOrdersDTO {
  requestId: string
  quoteVersion: string
  quoteHash: string
  orderUuids: string[]
  periodStart?: string
  periodEnd?: string
  settleDate?: string
  isInvoice?: number
  remark?: string
}

export interface SettleByMonthDTO {
  requestId: string
  quoteVersion: string
  quoteHash: string
  customerUuid: string
  periodStart: string
  periodEnd: string
  settleDate?: string
  isInvoice?: number
  remark?: string
}

export interface SettleQuoteVO {
  quoteVersion: string
  quoteHash: string
  orderCount: number
  pendingPriceCount: number
  isInvoice: number
  sawAmount: number
  rewindAmount: number
  extraAmount: number
  amountNoTax: number
  taxAmount: number
  totalAmount: number
  lines: SettleQuoteLine[]
}

export interface SettleQuoteLine {
  orderUuid: string
  sawAmount: number
  rewindAmount: number
  /** Optional for quotes generated before pricing-audit fields were introduced. */
  standardProcessAmount?: number
  pricingAdjustmentAmount?: number
  extraAmount: number
  amountNoTax: number
  taxAmount: number
  totalAmount: number
}

export interface SettleQuoteByOrdersDTO {
  orderUuids: string[]
  isInvoice?: number
}

export interface SettleQuoteByMonthDTO {
  customerUuid: string
  periodStart: string
  periodEnd: string
  isInvoice?: number
}

export interface ReceiveDTO {
  requestId: string
  receiveAmount?: number
  cashAmount?: number
  scrapOffsetAmount?: number
  discountAmount?: number
  discountReason?: string
  discountApprovalUuid?: string
  scrapWeight?: number
  payMethod?: number
  payNo?: string
  receiveDate?: string
  remark?: string
}

export interface SettleDiscountApprovalRequestDTO {
  requestId: string
  discountAmount: number
  reason: string
}

export interface SettleDiscountApproval {
  uuid: string
  discountAmount: number
  reason: string
  approvalStatus: number
  requestByName: string
  requestTime: string
  approveByName?: string
  approveTime?: string
  usedReceiveUuid?: string
}

export type SettleCollectionQueue = 'today' | 'overdue' | 'upcoming' | 'reminded'

export interface SettleCollectionReminderRequestDTO {
  requestId: string
  reminderChannel: number
  reminderResult: number
  contactName?: string
  reminderTime?: string
  nextFollowUpDate?: string
  remark: string
}

export interface SettleCollectionReminder {
  uuid: string
  reminderChannel: number
  reminderResult: number
  contactName?: string
  reminderTime: string
  nextFollowUpDate?: string
  operatorName: string
  remark: string
}

export interface SettleCollectionSummary {
  dueTodayCount: number
  dueTodayAmount: number
  overdueCount: number
  overdueAmount: number
  upcomingCount: number
  upcomingAmount: number
  remindedTodayCount: number
  remindedTodayAmount: number
  asOf?: string
}

export interface SettleActionReasonDTO {
  reason: string
}

export interface SettleQuery extends PageQuery {
  keyword?: string
  customerUuid?: string
  settleStatus?: number
  settleType?: number
  dateFrom?: string
  dateTo?: string
  collectionQueue?: SettleCollectionQueue
}

export interface SettleListSummary {
  totalDocumentCount: number
  pendingDocumentCount: number
  partialDocumentCount: number
  paidDocumentCount: number
  voidDocumentCount: number
  activeTotalAmount: number
  activeReceivedAmount: number
  activeUnreceivedAmount: number
  activeDiscountAmount: number
  asOf?: string
}
