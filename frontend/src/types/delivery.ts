import type { PageQuery } from './common'
import type { OperationLog } from './operationLog'

export interface DeliveryOrder {
  uuid: string
  deliveryNo: string
  customerUuid: string
  customerName: string
  deliveryDate: string
  totalCount: number
  totalWeight: number
  pickerName?: string
  carNo?: string
  containerNo?: string
  signUser?: string
  signTime?: string
  settleBlockAction: number
  deliveryStatus: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface DeliveryDetail {
  uuid: string
  deliveryUuid: string
  finishUuid: string
  orderUuid: string
  orderNo?: string
  finishRollNo: string
  paperName: string
  gramWeight?: number
  finishWidth?: number
  finishDiameter?: number
  finishCoreDiameter?: number
  actualWeight?: number
  outWeight: number
  sourceType?: number
  finishStatus?: number
  originalRollNos?: string
  originalSummary?: string
  processModeText?: string
  processSummary?: string
  remark?: string
  finishRemark?: string
  actualRemark?: string
}

export interface AvailableFinishVO {
  finishUuid: string
  finishRollNo: string
  orderUuid: string
  orderNo: string
  orderDate?: string
  paperName: string
  gramWeight?: number
  finishWidth?: number
  finishDiameter?: number
  finishCoreDiameter?: number
  actualWeight: number
  sourceType: number
  finishStatus: number
  settleType?: number
  settleDay?: number
  isInvoice?: number
  settlementRisk?: boolean
}

export interface DeliveryDetailVO {
  order: DeliveryOrder
  details: DeliveryDetail[]
  operationLogs?: OperationLog[]
}

export interface DeliveryCreateDTO {
  customerUuid: string
  deliveryDate: string
  pickerName?: string
  carNo?: string
  containerNo?: string
  remark?: string
  forceRelease: boolean
  items: DeliveryCreateItemDTO[]
}

export interface DeliveryAppendItemsDTO {
  forceRelease: boolean
  items: DeliveryCreateItemDTO[]
}

export interface DeliveryCreateItemDTO {
  finishUuid: string
  outWeight?: number
  remark?: string
}

export interface DeliveryConfirmDTO {
  signUser?: string
  signTime?: string
  remark?: string
}

export interface DeliveryRollbackDTO {
  reason: string
}

export interface DeliveryQuery extends PageQuery {
  keyword?: string
  customerUuid?: string
  deliveryStatus?: number
  dateFrom?: string
  dateTo?: string
}
