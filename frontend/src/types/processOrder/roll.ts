import type { WidthDifferencePolicy } from './widthDifferencePolicy'

/** 原纸明细，与后端 OriginalRoll 对应。 */
export interface OriginalRoll {
  uuid: string
  orderUuid?: string
  rowSort?: number
  extraNo?: string
  rollNo?: string
  paperName?: string
  gramWeight?: number
  actualGramWeight?: number
  originalWidth?: number
  actualWidth?: number
  originalDiameter?: number
  coreDiameter?: number
  originalLength?: number
  rollWeight?: number
  actualWeight?: number
  pieceNum?: number
  totalWeight?: number
  batchNo?: string
  damageDesc?: string
  damageImages?: string | string[]
  /** 1标准加工 2现场定尺 3不加工直发 4仅附加工艺 */
  processMode?: number
  /** 主工艺类型：1锯纸 2复卷 */
  mainStepType?: number
  /** 1待加工 2加工中 3完成 4直发 5报废 */
  rollStatus?: number
  /** 0未回录 1已完成回录 */
  isChecked?: number
  checkUser?: string
  checkTime?: string
  machineUuid?: string
  operator?: string
  processAmount?: number
  remark?: string
  serviceSteps?: ProcessStep[]
}

/** 成品明细（详情只读展示用，最小字段集）。 */
export interface FinishRoll {
  uuid: string
  rowSort?: number
  finishRollNo?: string
  /** 1预生成 2已使用 3作废 */
  rollNoStatus?: number
  /** 0正式 1备用 */
  isSpare?: number
  /** 1加工产出 2原纸直发 3仅附加工艺产出 */
  sourceType?: number
  paperName?: string
  customerPaperName?: string
  gramWeight?: number
  customerGramWeight?: number
  finishWidth?: number
  customerFinishWidth?: number
  customerSpecOverrideReason?: string
  customerSpecOverrideBy?: string
  customerSpecOverrideAt?: string
  finishDiameter?: number
  finishCoreDiameter?: number
  trimWidthShare?: number
  estimateWeight?: number
  actualWeight?: number
  scrapWeight?: number
  /** 0正品 1边角余料 */
  isRemain?: number
  isAbnormal?: number
  abnormalType?: string
  actualRemark?: string
  /** 1计划产出 2正常产出 3计划未产出 4实际新增产出 */
  productionResult?: number
  productionAdjustmentReason?: string
  /** 1待入库 2已入库 3已出库 4报废 */
  finishStatus?: number
  remark?: string
}

/** 工序明细（详情只读展示用，最小字段集）。 */
export interface ProcessStep {
  uuid: string
  originalUuid?: string
  /** 1原纸 2上一阶段产出 */
  inputType?: number
  inputOutputUuid?: string
  stageLevel?: number
  parentStepUuid?: string
  stepSort?: number
  /** 1锯纸 2复卷 3剥损整理 4重新包装 */
  stepType?: number
  stepName?: string
  machineUuid?: string
  machineNameSnap?: string
  /** 1主工艺 0追加工序 */
  isMain?: number
  knifeCount?: number
  processWeight?: number
  /** 服务工序计费基准。 */
  billingBasis?: string
  serviceQuantity?: number
  /** 标准单价快照。 */
  unitPrice?: number
  /** 人工核定单价，为空时沿用标准单价。 */
  billingUnitPrice?: number
  stepAmount?: number
  /** 1标准计价 2指定数量 3固定金额 4免收 */
  billingMode?: 1 | 2 | 3 | 4
  standardQuantity?: number
  billingQuantity?: number
  billingAmount?: number
  standardStepAmount?: number
  pricingAdjustmentAmount?: number
  pricingAdjustmentReason?: string
  pricingAdjustedBy?: string
  pricingAdjustedAt?: string
  pricingAdjustmentBatchId?: string
  widthDifferencePolicy?: WidthDifferencePolicy
  plannedLossWidth?: number
  plannedLossWeight?: number
  lossWeight?: number
  operator?: string
  remark?: string
}
