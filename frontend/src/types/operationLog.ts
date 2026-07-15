export interface OperationLog {
  uuid: string
  bizType: string
  bizUuid: string
  bizNo?: string
  actionType: string
  fieldName?: string
  oldValue?: string
  newValue?: string
  operator: string
  operateTime: string
  remark?: string
}

export interface OperationLogQuery {
  current?: number
  size?: number
  bizType?: string
  bizNo?: string
  actionType?: string
  operator?: string
  fieldName?: string
  remark?: string
  dateFrom?: string
  dateTo?: string
}
