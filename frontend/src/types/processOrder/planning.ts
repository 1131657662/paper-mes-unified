import type {
  FinishConfigSpecDTO,
  FinishLayerDTO,
  RewindFinishItemPreview,
  RewindSegmentPreview,
} from './finishConfig'
import type { WidthDifferencePolicy } from './widthDifferencePolicy'

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
  customerPaperName?: string
  customerGramWeight?: number
  customerFinishWidth?: number
  customerSpecOverrideReason?: string
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
  allocationRule?: 'WEIGHT_SPLIT'
  widthDifferencePolicy?: WidthDifferencePolicy
  remark?: string
  finishSpecs?: FinishConfigSpecDTO[]
  segments?: RewindSegmentPlanDTO[]
}

export interface ProcessPlanPreviewRequestDTO {
  expectedVersion: number
  originalUuid: string
  plan: ProcessPlanDTO
}

export interface ProcessPlanBatchSaveDTO {
  expectedVersion: number
  originalUuids: string[]
  plan: ProcessPlanDTO
}

export interface ProcessPlanBatchItemDTO {
  originalUuid: string
  plan: ProcessPlanDTO
}

export interface ProcessPlanItemsBatchSaveDTO {
  expectedVersion: number
  items: ProcessPlanBatchItemDTO[]
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
  weightPending?: boolean
  widthDifferencePolicy?: WidthDifferencePolicy
  widthDifference?: number
  widthDifferenceWeight?: number
  calculatedLossWeight?: number
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
  /** 0正品 1边角余料 */
  isRemain?: number
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
  machineUuid?: string
  knifeCount?: number
  processWeight?: number
  unitPrice?: number
  plan?: ProcessPlanDTO
  outputs?: ProcessRouteOutputDTO[]
}

export interface ProcessRoutePreviewDTO {
  expectedVersion?: number
  originalUuid: string
  stages: ProcessRouteStageDTO[]
}

export interface ProcessRouteBatchSaveDTO {
  expectedVersion: number
  routes: ProcessRoutePreviewDTO[]
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
  widthDifferencePolicy?: WidthDifferencePolicy
  plannedLossWidth?: number
  plannedLossWeight?: number
}

export interface ProcessRouteOutputVO {
  outputKey?: string
  stageLevel?: number
  outputSort?: number
  outputType?: number
  consumedByNextStage?: boolean
  /** 0正品 1边角余料 */
  isRemain?: number
  paperName?: string
  gramWeight?: number
  finishWidth?: number
  finishDiameter?: number
  finishCoreDiameter?: number
  actualWeight?: number
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
  configType?: 'singlePlan' | 'routePlan'
  plan?: ProcessPlanDTO
  preview?: PlanPreviewVO
  route?: ProcessRoutePreviewDTO
  routePreview?: ProcessRoutePreviewVO
}
