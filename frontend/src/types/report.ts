export interface ReportQuery {
  dateFrom?: string
  dateTo?: string
  customerUuid?: string
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
