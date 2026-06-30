export interface ReportQuery {
  dateFrom?: string
  dateTo?: string
  customerUuid?: string
  paperName?: string
  mainStepType?: number
  processMode?: number
  machineUuid?: string
  settleType?: number
  isInvoice?: number
  orderStatus?: number
  dimension?: ReportDimension
}

export type ReportDimension =
  | 'month'
  | 'customer'
  | 'paper'
  | 'process'
  | 'machine'
  | 'invoice'
  | 'settleType'
  | 'status'

export interface ReportOverviewVO {
  orderCount: number
  originalRollCount: number
  finishRollCount: number
  originalWeight: number
  finishWeight: number
  lossWeight: number
  lossRatio: number
  knifeCount: number
  sawAmount: number
  rewindAmount: number
  processAmount: number
  extraAmount: number
  totalAmount: number
  settledAmount: number
  pendingSettleAmount: number
  receivedAmount: number
  unreceivedAmount: number
}

export interface ReportDimensionVO {
  dimensionKey: string
  dimensionName: string
  orderCount: number
  originalRollCount: number
  finishRollCount: number
  originalWeight: number
  finishWeight: number
  lossWeight: number
  lossRatio: number
  knifeCount: number
  sawAmount: number
  rewindAmount: number
  processAmount: number
  extraAmount: number
  totalAmount: number
  settledAmount: number
  pendingSettleAmount: number
  receivedAmount: number
  unreceivedAmount: number
}

export interface ReportDetailVO {
  orderUuid: string
  orderNo: string
  orderDate: string
  customerName: string
  settleType: number
  isInvoice: number
  orderStatus: number
  originalRollCount: number
  finishRollCount: number
  paperSummary: string
  processSummary: string
  originalWeight: number
  finishWeight: number
  lossWeight: number
  lossRatio: number
  knifeCount: number
  sawAmount: number
  rewindAmount: number
  processAmount: number
  extraAmount: number
  totalAmount: number
  settledAmount: number
  pendingSettleAmount: number
  receivedAmount: number
  unreceivedAmount: number
}

export interface MonthlyReportVO {
  month: string
  orderCount: number
  totalTon: number
  totalKnife: number
  totalAmount: number
  totalFinishWeight: number
}

export interface CustomerReportVO {
  customerUuid: string
  customerName: string
  orderCount: number
  totalTon: number
  totalKnife: number
  totalAmount: number
}

export interface LossReportVO {
  month: string
  rollCount: number
  totalOriginalWeight: number
  totalLossWeight: number
  avgLossRatio: number
}

export interface MachineReportVO {
  machineUuid: string
  machineName: string
  rollCount: number
  totalOutputWeight: number
  totalKnife: number
  totalLossWeight: number
}
