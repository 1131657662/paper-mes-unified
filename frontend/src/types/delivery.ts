import type { PageQuery } from './common'

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
  finishRollNo: string
  paperName: string
  outWeight: number
  remark?: string
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

export interface DeliveryQuery extends PageQuery {
  keyword?: string
  customerUuid?: string
  deliveryStatus?: number
}
