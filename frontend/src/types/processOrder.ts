import type { PageQuery } from './common'

/** 加工单主表，与后端 ProcessOrder 对应（含 BaseEntity 通用字段，按需取用）。 */
export interface ProcessOrder {
  uuid: string
  orderNo?: string
  customerUuid?: string
  customerName?: string
  orderDate?: string
  expectFinishDate?: string
  /** 1普通 2加急 3特急 */
  priority?: number
  labelBrand?: string
  warehouseUuid?: string
  teamGroup?: string
  /** 1开票 2不开票 */
  isInvoice?: number
  /** 1次结 2月结，本单可覆盖客户默认值。 */
  settleType?: number
  settleDay?: number
  taxRate?: number
  urgentFee?: number
  palletFee?: number
  loadingFee?: number
  freightFee?: number
  otherFee?: number
  totalProcessAmount?: number
  totalExtraAmount?: number
  totalAmountNoTax?: number
  totalAmountTax?: number
  totalAmount?: number
  totalOriginalWeight?: number
  totalOriginalTon?: number
  totalFinishWeight?: number
  originalRollCount?: number
  originalPieceCount?: number
  originalRollWeight?: number
  finishRollCount?: number
  finishRollWeight?: number
  estimateFinishWeight?: number
  actualFinishWeight?: number
  spareRollCount?: number
  actualTotalKnife?: number
  /** 0草稿 1待下发 2加工中 3待回录 4已完成 5已结算 6已作废 */
  orderStatus?: number
  /** 0未打印 1已打印 */
  printStatus?: number
  printCount?: number
  /** 0单一工艺 1混合 */
  isMixProcess?: number
  remark?: string
  remarkLong?: string
  voidTime?: string
  voidUser?: string
  voidReason?: string
  createTime?: string
  updateTime?: string
}

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
  /** 1标准加工 2现场定尺 3不加工直发 */
  processMode?: number
  /** 主工艺类型：1锯纸 2复卷 */
  mainStepType?: number
  /** 1待加工 2加工中 3完成 4直发 5报废 */
  rollStatus?: number
  machineUuid?: string
  operator?: string
  processAmount?: number
  remark?: string
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
  /** 1加工产出 2原纸直发 */
  sourceType?: number
  paperName?: string
  gramWeight?: number
  finishWidth?: number
  estimateWeight?: number
  actualWeight?: number
  scrapWeight?: number
  /** 0正品 1边角余料 */
  isRemain?: number
  isAbnormal?: number
  abnormalType?: string
  actualRemark?: string
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
  /** 1锯纸 2复卷 */
  stepType?: number
  stepName?: string
  /** 1主工艺 0追加工序 */
  isMain?: number
  knifeCount?: number
  processWeight?: number
  unitPrice?: number
  stepAmount?: number
  lossWeight?: number
  operator?: string
  remark?: string
}

export interface FinishSourceVO {
  originalUuid?: string
  rollNo?: string
  paperName?: string
  shareRatio?: number
  shareWeight?: number
  remark?: string
}

export interface FinishProductionVO {
  uuid: string
  finishRollNo?: string
  rowSort?: number
  rollNoStatus?: number
  isSpare?: number
  sourceType?: number
  paperName?: string
  gramWeight?: number
  finishWidth?: number
  finishDiameter?: number
  finishCoreDiameter?: number
  estimateWeight?: number
  actualWeight?: number
  trimWeightShare?: number
  finishStatus?: number
  sources?: FinishSourceVO[]
}

export interface StageOutputVO {
  uuid: string
  outputNo?: string
  finishRollUuid?: string
  parentOutputUuid?: string
  stageLevel?: number
  outputSort?: number
  outputType?: number
  outputStatus?: number
  paperName?: string
  gramWeight?: number
  finishWidth?: number
  finishDiameter?: number
  finishCoreDiameter?: number
  estimateWeight?: number
  actualWeight?: number
  sourceStepType?: number
  sourceSummary?: string
}

export interface RewindParamVO {
  paramMode?: number
  layerSort?: number
  outDiameter?: number
  coreDiameter?: number
  layerWidth?: number
  areaRatio?: number
  splitRatio?: number
  remark?: string
}

export interface RollProductionVO {
  originalUuid?: string
  extraNo?: string
  batchNo?: string
  rollNo?: string
  damageDesc?: string
  paperName?: string
  gramWeight?: number
  originalWidth?: number
  rollWeight?: number
  processAmount?: number
  pieceNum?: number
  processMode?: number
  mainStepType?: number
  rollStatus?: number
  remark?: string
  steps?: ProcessStep[]
  stageOutputs?: StageOutputVO[]
  rewindParams?: RewindParamVO[]
  finishes?: FinishProductionVO[]
}

/** 加工单详情返回体，与后端 ProcessOrderDetailVO 对应。 */
export interface ProcessOrderDetailVO {
  order: ProcessOrder
  originalRolls: OriginalRoll[]
  rolls: OriginalRoll[]
  finishRolls: FinishRoll[]
  steps: ProcessStep[]
  rollProductions?: RollProductionVO[]
}

/** 原纸明细行入参，与后端 OriginalRollDTO 对应。 */
export interface OriginalRollDTO {
  extraNo?: string
  rollNo?: string
  paperName: string
  gramWeight: number
  originalWidth: number
  originalDiameter?: number
  coreDiameter?: number
  originalLength?: number
  rollWeight: number
  pieceNum?: number
  batchNo?: string
  damageDesc?: string
  /** 1标准加工 2现场定尺 3不加工直发 */
  processMode?: number
  /** 主工艺类型：1锯纸 2复卷 */
  mainStepType?: number
  machineUuid?: string
  remark?: string
}

export interface FinishLayerDTO {
  outDiameter?: number
  coreDiameter?: number
}

export interface FinishSourceDTO {
  originalUuid?: string
  shareRatio?: number
  consumeRatio?: number
}

export interface FinishConfigSpecDTO {
  itemType?: 'FINISH' | 'TRIM'
  finishWidth?: number
  finishDiameter?: number
  finishCoreDiameter?: number
  count: number
  estimateWeight?: number
  splitRatio?: number
  sources?: FinishSourceDTO[]
  layers?: FinishLayerDTO[]
}

export interface FinishConfigSaveDTO {
  processMode: number
  mainStepType?: number
  spareCount?: number
  rewindMode?: number
  knifeCount?: number
  unitPrice?: number
  finishSpecs?: FinishConfigSpecDTO[]
  rewindSegments?: RewindSegmentDTO[]
}

export interface FinishConfigSaveVO {
  orderUuid?: string
  originalUuid?: string
  finishRollNos?: string[]
  spareRollNos?: string[]
}

export interface RewindLayoutItemDTO {
  width: number
  quantity?: number
  itemType?: 'FINISH' | 'TRIM'
  layers?: FinishLayerDTO[]
}

export interface RewindSegmentDTO {
  segmentSort?: number
  segmentRatio?: number
  targetDiameter?: number
  finishCoreDiameter?: number
  repeatCount?: number
  sources?: FinishSourceDTO[]
  layoutItems?: RewindLayoutItemDTO[]
}

export interface RewindPlanPreviewDTO {
  rewindMode: number
  spareCount?: number
  segments?: RewindSegmentDTO[]
}

export interface RewindSegmentPreview {
  segmentSort?: number
  segmentRatio?: number
  targetDiameter?: number
  repeatCount?: number
  layoutWidth?: number
  trimWidth?: number
  summary?: string
}

export interface RewindFinishItemPreview {
  segmentSort?: number
  finishWidth?: number
  finishDiameter?: number
  finishCoreDiameter?: number
  segmentRatio?: number
  estimateWeight?: number
  trimWidth?: number
  trimWeight?: number
  sourceSummary?: string
  layers?: FinishLayerDTO[]
}

export interface FinishPreviewVO {
  originalUuid?: string
  rewindMode?: number
  originalWidth?: number
  finishCount?: number
  trimCount?: number
  spareCount?: number
  totalEstimateWeight?: number
  totalTrimWeight?: number
  segments?: RewindSegmentPreview[]
  finishes?: RewindFinishItemPreview[]
}

export interface RewindSourcePlanDTO {
  originalUuid?: string
  sourceSort?: number
  shareRatio?: number
  consumeRatio?: number
  shareWeight?: number
  remark?: string
}

export interface RewindLayoutItemPlanDTO {
  width: number
  quantity?: number
  itemType?: 'FINISH' | 'TRIM'
  layers?: FinishLayerDTO[]
}

export interface RewindSegmentPlanDTO {
  segmentSort?: number
  segmentRatio?: number
  targetDiameter?: number
  finishCoreDiameter?: number
  repeatCount?: number
  sources?: RewindSourcePlanDTO[]
  layoutItems?: RewindLayoutItemPlanDTO[]
}

export interface ProcessPlanDTO {
  processMode: number
  mainStepType?: number
  machineUuid?: string
  spareCount?: number
  rewindMode?: number
  knifeCount?: number
  unitPrice?: number
  remark?: string
  finishSpecs?: FinishConfigSpecDTO[]
  segments?: RewindSegmentPlanDTO[]
}

export interface ProcessPlanPreviewRequestDTO {
  originalUuid: string
  plan: ProcessPlanDTO
}

export interface ProcessPlanBatchSaveDTO {
  originalUuids: string[]
  plan: ProcessPlanDTO
}

export interface PlanPreviewVO {
  originalUuid?: string
  processMode?: number
  mainStepType?: number
  rewindMode?: number
  finishCount?: number
  trimCount?: number
  spareCount?: number
  totalEstimateWeight?: number
  totalTrimWeight?: number
  summary?: string
  ready?: boolean
  errors?: string[]
  segments?: RewindSegmentPreview[]
  finishes?: RewindFinishItemPreview[]
}

export interface ProcessRouteOutputDTO {
  outputKey?: string
  outputType?: number
  count?: number
  paperName?: string
  gramWeight?: number
  finishWidth?: number
  finishDiameter?: number
  finishCoreDiameter?: number
  estimateWeight?: number
  remark?: string
}

export interface ProcessRouteStageDTO {
  stageLevel: number
  inputOutputKeys?: string[]
  stepType: number
  stepName?: string
  knifeCount?: number
  processWeight?: number
  unitPrice?: number
  outputs?: ProcessRouteOutputDTO[]
}

export interface ProcessRoutePreviewDTO {
  originalUuid: string
  stages: ProcessRouteStageDTO[]
}

export interface ProcessRouteStageLineVO {
  stageLevel?: number
  stepType?: number
  stepName?: string
  inputOutputKeys?: string[]
  knifeCount?: number
  processWeight?: number
  unitPrice?: number
  stepAmount?: number
}

export interface ProcessRouteOutputVO {
  outputKey?: string
  stageLevel?: number
  outputSort?: number
  outputType?: number
  consumedByNextStage?: boolean
  paperName?: string
  gramWeight?: number
  finishWidth?: number
  finishDiameter?: number
  finishCoreDiameter?: number
  estimateWeight?: number
  remark?: string
}

export interface ProcessRoutePreviewVO {
  originalUuid?: string
  totalAmount?: number
  stages?: ProcessRouteStageLineVO[]
  outputs?: ProcessRouteOutputVO[]
}

export interface ProcessConfigDraftVO {
  originalUuid?: string
  processMode?: number
  mainStepType?: number
  configStatus?: number
  lastError?: string
  plan?: ProcessPlanDTO
  preview?: PlanPreviewVO
}

export interface DraftOrderVO {
  order?: ProcessOrder
  currentStep?: number
  rolls?: OriginalRoll[]
  configs?: ProcessConfigDraftVO[]
}

export interface DraftSummaryVO {
  orderUuid?: string
  orderNo?: string
  customerName?: string
  orderDate?: string
  currentStep?: number
  rollCount?: number
  configuredCount?: number
  totalWeight?: number
}

/** 创建加工单入参，与后端 ProcessOrderCreateDTO 对应。 */
export interface ProcessOrderCreateDTO {
  customerUuid: string
  orderDate: string
  expectFinishDate?: string
  priority?: number
  labelBrand?: string
  warehouseUuid?: string
  teamGroup?: string
  isInvoice?: number
  settleType?: number
  settleDay?: number
  taxRate?: number
  urgentFee?: number
  palletFee?: number
  loadingFee?: number
  freightFee?: number
  otherFee?: number
  remark?: string
  remarkLong?: string
  originalRolls: OriginalRollDTO[]
}

export interface DraftOrderBaseDTO {
  customerUuid: string
  orderDate: string
  expectFinishDate?: string
  priority?: number
  labelBrand?: string
  warehouseUuid?: string
  teamGroup?: string
  isInvoice?: number
  settleType?: number
  settleDay?: number
  taxRate?: number
  urgentFee?: number
  palletFee?: number
  loadingFee?: number
  freightFee?: number
  otherFee?: number
  remark?: string
  remarkLong?: string
}

export interface OriginalRollBatchSaveDTO {
  rolls: OriginalRollDTO[]
}

export interface ProcessConfigDraftSaveDTO {
  config: FinishConfigSaveDTO
}

export interface DraftProgressDTO {
  currentStep?: number
}

export interface OriginalRollImportError {
  rowNumber: number
  field?: string
  message?: string
  raw?: Record<string, string>
}

export interface OriginalRollImportPreviewVO {
  validRows?: OriginalRollDTO[]
  errors?: OriginalRollImportError[]
}

export interface ProcessOrderSubmitVO {
  orderUuid?: string
  orderNo?: string
  orderStatus?: number
  finishRollNos?: string[]
  spareRollNos?: string[]
}

/** 通用状态变更入参。 */
export interface StatusChangeDTO {
  targetStatus: number
  reason?: string
}

/** 加工单作废入参。 */
export interface ProcessOrderVoidDTO {
  reason: string
}

/** 主单备注轻量编辑入参。 */
export interface ProcessOrderRemarkDTO {
  remark?: string
  remarkLong?: string
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

/** 打印结果，与后端 PrintResultVO 对应。 */
export interface PrintResultVO {
  orderUuid?: string
  orderNo?: string
  printCount?: number
  /** 是否补打（printCount>1） */
  reprint?: boolean
  printTime?: string
  orderStatus?: number
  finishRollNos?: string[]
  spareRollNos?: string[]
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
}

/** 成品快照差异项。 */
export interface FinishDiff {
  uuid?: string
  finishRollNo?: string
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
export interface BackRecordFinishDTO {
  uuid: string
  actualWeight?: number
  scrapWeight?: number
  isRemain?: number
  isAbnormal?: number
  abnormalType?: string
  actualRemark?: string
}

/** 工序损耗回录入参。 */
export interface BackRecordStepDTO {
  uuid: string
  lossWeight?: number
}

/** 整单回录入参。 */
export interface BackRecordDTO {
  operator?: string
  overToleranceAuthorized?: boolean
  releaseReason?: string
  rolls: BackRecordRollDTO[]
  finishes?: BackRecordFinishDTO[]
  steps?: BackRecordStepDTO[]
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
  overToleranceReleased?: boolean
  directShipGenerated?: number
  voidedSpareCount?: number
  rollChecks?: RollCheck[]
}

/** 批量生成正式成品卷号入参。 */
export interface FinishRollBatchDTO {
  count: number
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
}

/** 批量作废卷号入参。 */
export interface SpareRollBatchVoidDTO {
  uuids: string[]
}
