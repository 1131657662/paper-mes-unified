import type { PageQuery } from './common'
import type { OperationLog } from './operationLog'

export interface SettleOrder {
  uuid: string
  settleNo: string
  customerUuid: string
  customerName: string
  settleType: number
  settleDate: string
  periodStart?: string
  periodEnd?: string
  sawAmount: number
  rewindAmount: number
  extraAmount: number
  amountNoTax: number
  taxAmount: number
  totalAmount: number
  receivedAmount: number
  unreceivedAmount: number
  isInvoice: number
  settleStatus: number
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
  originalWeight?: number
  processMode?: number
  mainStepType?: number
  processText?: string
  finishSummary?: string
  finishCount?: number
  finishWeight?: number
  trimWeight?: number
  sawWeight?: number
  rewindWeight?: number
  sawUnitPrice?: number
  sawInvoiceUnitPrice?: number
  rewindUnitPrice?: number
  rewindInvoiceUnitPrice?: number
  sawAmount?: number
  rewindAmount?: number
  processAmount?: number
  extraAmount?: number
  extraFeeSummary?: string
  taxAmount?: number
  lineAmount?: number
  isInvoice?: number
  remark?: string
}

export interface ReceiveRecord {
  uuid: string
  settleUuid: string
  receiveDate: string
  receiveAmount: number
  payMethod: number
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
  customerUuid?: string
  periodStart?: string
  periodEnd?: string
}

export interface SettleCandidateVO {
  orderUuid: string
  orderNo: string
  customerUuid: string
  customerName: string
  orderDate?: string
  settleType?: number
  settleDay?: number
  isInvoice?: number
  originalRollCount?: number
  originalRollWeight?: number
  finishRollCount?: number
  finishRollWeight?: number
  sawAmount?: number
  rewindAmount?: number
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
  orderUuids: string[]
  periodStart?: string
  periodEnd?: string
  settleDate?: string
  isInvoice?: number
  remark?: string
}

export interface SettleByMonthDTO {
  customerUuid: string
  periodStart: string
  periodEnd: string
  settleDate?: string
  isInvoice?: number
  remark?: string
}

export interface ReceiveDTO {
  receiveAmount: number
  payMethod: number
  payNo?: string
  operator?: string
  receiveDate?: string
  remark?: string
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
}
