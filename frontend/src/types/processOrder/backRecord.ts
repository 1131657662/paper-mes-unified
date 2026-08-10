import type { PageQuery } from '../common'

/** 加工单列表查询入参。 */
export interface ProcessOrderQuery extends PageQuery {
  keyword?: string
  orderStatus?: number
  customerUuid?: string
  dateFrom?: string
  dateTo?: string
}

/** 原纸单卷回录入参。 */
export interface BackRecordRollDTO {
  uuid: string
  actualGramWeight?: number
  actualWidth?: number
  actualWeight?: number
  remark?: string
}

/** 成品卷回录入参。 */
export type BackRecordFinishAction = 'PRODUCED' | 'NOT_PRODUCED' | 'ADDED'

export interface BackRecordAddedFinishValues {
  uuid: string
  originalUuid: string
  finishWidth?: number
  finishDiameter?: number
  finishCoreDiameter?: number
  actualWeight?: number
  scrapWeight?: number
  isAbnormal?: number
  abnormalType?: string
  actualRemark?: string
}

export interface BackRecordFinishAdjustmentValues {
  plannedFinishUuids: string[]
  producedFinishUuids: string[]
  reason: string
  added: BackRecordAddedFinishValues[]
}

export interface BackRecordFinishDTO {
  uuid?: string
  originalUuid?: string
  productionAction?: BackRecordFinishAction
  productionAdjustmentReason?: string
  finishWidth?: number
  finishDiameter?: number
  finishCoreDiameter?: number
  actualWeight?: number
  scrapWeight?: number
  isRemain?: number
  isAbnormal?: number
  abnormalType?: string
  actualRemark?: string
}

export interface BackRecordTrimDTO {
  originalUuid: string
  finishWidth: number
  actualWeight: number
  actualRemark?: string
}

/** 工序损耗回录入参。 */
export interface BackRecordStepDTO {
  uuid: string
  lossWeight?: number
  knifeCount?: number
}

/** 整单回录入参。 */
export interface BackRecordDTO {
  expectedVersion: number
  completeOrder: boolean
  warehouseUuid: string
  releaseAdminUsername?: string
  releaseAdminPassword?: string
  releaseReason?: string
  varianceReason?: string
  rolls: BackRecordRollDTO[]
  finishes?: BackRecordFinishDTO[]
  trims?: BackRecordTrimDTO[]
  steps?: BackRecordStepDTO[]
}

export interface BackRecordReopenDTO {
  expectedVersion: number
  rollUuids: string[]
}

/** 单卷闭合校验结论（实际为整单聚合）。 */
export interface RollCheck {
  originalUuid?: string
  rollNo?: string
  level?: string
  actualWeight?: number
  theoreticalWeight?: number
  diffWeight?: number
  diffRatioPct?: number
}

/** 整单回录结果。 */
export interface BackRecordResultVO {
  orderUuid?: string
  orderNo?: string
  orderStatus?: number
  backRecordTime?: string
  orderCompleted?: boolean
  recordedRollCount?: number
  remainingRollCount?: number
  overToleranceReleased?: boolean
  directShipGenerated?: number
  voidedSpareCount?: number
  rollChecks?: RollCheck[]
}

/** 批量生成正式成品卷号入参。 */
export interface FinishRollBatchDTO {
  count: number
  originalUuid?: string
  paperName?: string
  customerPaperName?: string
  gramWeight?: number
  finishWidth?: number
  finishDiameter?: number
  finishCoreDiameter?: number
  warehouseUuid?: string
  remark?: string
}

/** 追加备用卷号入参。 */
export interface SpareRollAppendDTO {
  count: number
  originalUuid?: string
}

/** 批量作废卷号入参。 */
export interface SpareRollBatchVoidDTO {
  uuids: string[]
}
