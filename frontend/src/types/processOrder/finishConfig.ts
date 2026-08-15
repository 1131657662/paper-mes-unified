import type { ProcessStep } from './roll'
import type { WidthDifferencePolicy } from './widthDifferencePolicy'

/** 原纸明细行入参，与后端 OriginalRollDTO 对应。 */
export interface OriginalRollDTO {
  uuid?: string
  extraNo?: string
  rollNo?: string
  paperName: string
  gramWeight: number
  originalWidth: number
  actualWidth?: number
  originalDiameter?: number
  coreDiameter?: number
  originalLength?: number
  rollWeight?: number
  /** UNKNOWN has no reference value; ESTIMATED is a positive, not-yet-weighed reference value. */
  weightStatus?: 'UNKNOWN' | 'ESTIMATED' | 'MEASURED'
  pieceNum?: number
  batchNo?: string
  damageDesc?: string
  /** 1标准加工 2现场定尺 3不加工直发 4仅附加工艺 */
  processMode?: number
  /** 主工艺类型：1锯纸 2复卷 */
  mainStepType?: number
  machineUuid?: string
  remark?: string
  serviceSteps?: ProcessStep[]
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
  customerPaperName?: string
  customerGramWeight?: number
  customerFinishWidth?: number
  customerSpecOverrideReason?: string
  count: number
  estimateWeight?: number
  splitRatio?: number
  sources?: FinishSourceDTO[]
  layers?: FinishLayerDTO[]
}

export interface FinishConfigSaveDTO {
  processMode: number
  mainStepType?: number
  machineUuid?: string
  spareCount?: number
  rewindMode?: number
  knifeCount?: number
  unitPrice?: number
  widthDifferencePolicy?: WidthDifferencePolicy
  finishSpecs?: FinishConfigSpecDTO[]
  rewindSegments?: RewindSegmentDTO[]
}

export interface FinishConfigSaveVO {
  orderUuid?: string
  originalUuid?: string
  finishRollNos?: string[]
  spareRollNos?: string[]
}

export interface FinishConfigBatchSaveItemDTO {
  rollUuid: string
  config: FinishConfigSaveDTO
}

export interface FinishConfigBatchSaveDTO {
  items: FinishConfigBatchSaveItemDTO[]
}

export interface FinishConfigBatchSaveVO {
  orderUuid?: string
  results?: FinishConfigSaveVO[]
}

export interface RewindLayoutItemDTO {
  width: number
  quantity?: number
  itemType?: 'FINISH' | 'TRIM'
  customerPaperName?: string
  customerGramWeight?: number
  customerFinishWidth?: number
  customerSpecOverrideReason?: string
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
  widthDifferencePolicy?: WidthDifferencePolicy
  segments?: RewindSegmentDTO[]
}

export interface RewindSegmentPreview {
  segmentSort?: number
  segmentRatio?: number
  targetDiameter?: number
  repeatCount?: number
  layoutWidth?: number
  trimWidth?: number
  trimWeight?: number
  widthDifference?: number
  lossWeight?: number
  summary?: string
}

export interface RewindFinishItemPreview {
  segmentSort?: number
  finishWidth?: number
  finishDiameter?: number
  finishCoreDiameter?: number
  customerPaperName?: string
  customerGramWeight?: number
  customerFinishWidth?: number
  customerSpecOverrideReason?: string
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
  weightPending?: boolean
  widthDifferencePolicy?: WidthDifferencePolicy
  widthDifference?: number
  widthDifferenceWeight?: number
  calculatedLossWeight?: number
  segments?: RewindSegmentPreview[]
  finishes?: RewindFinishItemPreview[]
}
