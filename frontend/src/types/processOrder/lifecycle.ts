import type { PrintViewVersion } from './production'

/** 通用状态变更入参。 */
export interface StatusChangeDTO {
  targetStatus: number
  reason?: string
}

/** 加工单作废入参。 */
export interface ProcessOrderVoidDTO {
  reason: string
}

export interface ProcessOrderRollbackDTO {
  reason: string
}

/** 主单备注轻量编辑入参。 */
export interface ProcessOrderRemarkDTO {
  expectedVersion: number
  remark?: string
  remarkLong?: string
}

export type ProcessRollDispositionAction = 'DIRECT_SHIP' | 'CANCEL' | 'SPLIT_TO_ORDER'

export interface ProcessRollDispositionDTO {
  action: ProcessRollDispositionAction
  requestId: string
  reason: string
  expectedOrderVersion: number
  warehouseUuid?: string
  actualWeight?: number
}

export interface ProcessRollDispositionVO {
  sourceOrderUuid?: string
  sourceOrderNo?: string
  sourceRollUuid?: string
  action?: ProcessRollDispositionAction
  targetOrderUuid?: string
  targetOrderNo?: string
  targetRollUuid?: string
  targetFinishUuid?: string
  targetFinishUuids?: string[]
  operatedAt?: string
}

export interface ProcessOrderPostProductionNoteDTO {
  expectedVersion: number
  postProductionNote?: string
}

export type ProcessOrderIssueConsistencyStatus =
  | 'IN_SYNC'
  | 'REISSUE_REQUIRED'
  | 'PENDING_REISSUE'
  | 'NOT_APPLICABLE'

export interface ProcessOrderIssueConsistency {
  status: ProcessOrderIssueConsistencyStatus
  currentIssueVersion?: number
  changedGroups: string[]
  blockingReason?: string
  pendingDeliveryCount?: number
}

/** 原纸明细备注类字段轻量编辑入参。 */
export interface OriginalRollRemarkDTO {
  batchNo?: string
  damageDesc?: string
  remark?: string
}

/** 打印入参，与后端 PrintDTO 对应（首打可不传，补打需 reason）。 */
export interface PrintDTO {
  reason?: string
}

export interface PhysicalReprintDTO {
  reason: string
  version: PrintViewVersion
}

/** 打印结果，与后端 PrintResultVO 对应。 */
export interface PrintResultVO {
  orderUuid?: string
  orderNo?: string
  printCount?: number
  issueVersion?: number
  /** 0已下发但未确认物理打印，1已确认打印。 */
  printStatus?: number
  /** 是否补打（printCount>1） */
  reprint?: boolean
  printTime?: string
  orderStatus?: number
  finishRollNos?: string[]
  spareRollNos?: string[]
}

export interface ProcessOrderReissueDTO {
  requestId: string
  expectedVersion: number
  reason: string
}

export interface ProcessOrderIssueVersion {
  uuid?: string
  orderUuid: string
  versionNo?: number
  previousVersionNo?: number
  status: 'PENDING' | 'APPLIED' | 'ARCHIVED' | 'LEGACY_UNVERSIONED' | string
  changeReason?: string
  operatorName?: string
  changeTime?: string
  issueTime?: string
  issueOperatorName?: string
  hasSnapshotBefore: boolean
  hasSnapshotAfter: boolean
}

/** 单卷计费明细。 */
export interface RollFee {
  originalUuid?: string
  rollNo?: string
  processAmount?: number
}

/** 工序计费明细。 */
export interface StepFee {
  stepUuid?: string
  originalUuid?: string
  /** 1锯纸 2复卷 */
  stepType?: number
  unitPrice?: number
  /** 锯纸=刀数，复卷=吨位 */
  quantity?: number
  standardQuantity?: number
  standardStepAmount?: number
  billingMode?: number
  pricingAdjustmentAmount?: number
  stepAmount?: number
}

/** 整单计费结果，与后端 FeeResultVO 对应。 */
export interface FeeResultVO {
  orderUuid?: string
  orderNo?: string
  totalProcessAmount?: number
  totalExtraAmount?: number
  totalAmountNoTax?: number
  totalAmountTax?: number
  totalAmount?: number
  actualTotalKnife?: number
  /** 0单一工艺 1混合 */
  isMixProcess?: number
  rollFees?: RollFee[]
  stepFees?: StepFee[]
}

/** 原纸快照差异项。 */
export interface RollDiff {
  uuid?: string
  rollNo?: string
  printGramWeight?: number
  finishGramWeight?: number
  gramWeightChanged?: boolean
  printWidth?: number
  finishWidth?: number
  widthChanged?: boolean
  printWeight?: number
  finishWeight?: number
  weightChanged?: boolean
}

/** 成品快照差异项。 */
export interface FinishDiff {
  uuid?: string
  finishRollNo?: string
  printWidth?: number
  finishWidth?: number
  widthChanged?: boolean
  printDiameter?: number
  finishDiameter?: number
  diameterChanged?: boolean
  estimateWeight?: number
  actualWeight?: number
  weightChanged?: boolean
}

/** 下发vs完成快照对比结果，与后端 SnapshotDiffVO 对应。 */
export interface SnapshotDiffVO {
  orderUuid?: string
  orderNo?: string
  rollDiffs?: RollDiff[]
  finishDiffs?: FinishDiff[]
}
