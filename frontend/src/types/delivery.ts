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
  voidReason?: string
  voidBy?: string
  voidTime?: string
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
  remainingWeight?: number
  outWeight: number
  isRemain?: number
  sourceType?: number
  finishStatus?: number
  originalRollNos?: string
  originalSummary?: string
  processModeText?: string
  processSummary?: string
  originalItems?: DeliveryOriginalSourceItem[]
  processStepItems?: DeliveryProcessStepItem[]
  remark?: string
  finishRemark?: string
  actualRemark?: string
}

export interface DeliveryOriginalSourceItem {
  uuid?: string
  rowSort?: number
  extraNo?: string
  rollNo?: string
  paperName?: string
  gramWeight?: number
  actualGramWeight?: number
  originalWidth?: number
  actualWidth?: number
  actualWeight?: number
  totalWeight?: number
  processMode?: number
  mainStepType?: number
  machineUuid?: string
  machineName?: string
  operator?: string
  remark?: string
}

export interface DeliveryProcessStepItem {
  uuid?: string
  originalUuid?: string
  stepSort?: number
  stepType?: number
  stepName?: string
  isMain?: number
  knifeCount?: number
  processWeight?: number
  unitPrice?: number
  stepAmount?: number
  lossWeight?: number
  operator?: string
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
  remainingWeight?: number
  isRemain?: number
  sourceType: number
  finishStatus: number
  originalRollNos?: string
  sourceMotherRolls?: AvailableFinishSourceMotherRoll[]
  settleType?: number
  settleDay?: number
  isInvoice?: number
  settlementRisk?: boolean
}

export interface AvailableFinishSourceMotherRoll {
  originalUuid: string
  rowSort?: number
  rollNo?: string
  extraNo?: string
  paperName?: string
  gramWeight?: number
  originalWidth?: number
  actualWeight?: number
  allocationWeight?: number
}

export interface DeliveryDetailVO {
  order: DeliveryOrder
  details: DeliveryDetail[]
  operationLogs?: OperationLog[]
  rollbackSnapshot?: DeliveryRollbackSnapshotVO
}

export interface DeliveryRollbackSnapshotVO {
  deliveryNo?: string
  rollbackReason?: string
  rollbackOperator?: string
  rollbackTime?: string
  signUser?: string
  signTime?: string
  totalCount?: number
  totalWeight?: number
  details?: DeliveryDetail[]
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

export interface DeliveryBatchConfirmDTO extends DeliveryConfirmDTO {
  deliveryUuids: string[]
}

export interface DeliveryRollbackDTO {
  reason: string
}

export interface DeliveryCancelDTO {
  reason: string
}

export interface DeliveryQuery extends PageQuery {
  keyword?: string
  customerUuid?: string
  deliveryStatus?: number
  dateFrom?: string
  dateTo?: string
}

export interface DeliveryListSummary {
  totalDocumentCount: number
  pendingDocumentCount: number
  deliveredDocumentCount: number
  voidDocumentCount: number
  activeRollCount: number
  activeWeight: number
  pendingWeight: number
  deliveredWeight: number
}
