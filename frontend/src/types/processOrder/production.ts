import type { ProcessOrder } from './order'
import type { FinishRoll, OriginalRoll, ProcessStep } from './roll'

export interface FinishSourceVO {
  originalUuid?: string
  rowSort?: number
  extraNo?: string
  rollNo?: string
  paperName?: string
  gramWeight?: number
  actualGramWeight?: number
  originalWidth?: number
  actualWidth?: number
  rollWeight?: number
  weightStatus?: 'UNKNOWN' | 'ESTIMATED' | 'MEASURED'
  pieceNum?: number
  actualWeight?: number
  totalWeight?: number
  shareRatio?: number
  consumeRatio?: number
  shareWeight?: number
  remark?: string
}

export interface FinishProductionVO {
  uuid: string
  finishRollNo?: string
  rowSort?: number
  rollNoStatus?: number
  isSpare?: number
  isRemain?: number
  sourceType?: number
  paperName?: string
  gramWeight?: number
  finishWidth?: number
  finishDiameter?: number
  finishCoreDiameter?: number
  estimateWeight?: number
  actualWeight?: number
  trimWidthShare?: number
  trimWeightShare?: number
  actualRemark?: string
  finishStatus?: number
  productionResult?: number
  productionAdjustmentReason?: string
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
  /** 0正品 1边角余料 */
  isRemain?: number
  sourceStepType?: number
  sourceSummary?: string
  remark?: string
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
  rowSort?: number
  extraNo?: string
  batchNo?: string
  rollNo?: string
  damageDesc?: string
  damageImages?: string[]
  paperName?: string
  gramWeight?: number
  actualGramWeight?: number
  originalWidth?: number
  actualWidth?: number
  rollWeight?: number
  weightStatus?: 'UNKNOWN' | 'ESTIMATED' | 'MEASURED'
  actualWeight?: number
  processAmount?: number
  pieceNum?: number
  processMode?: number
  mainStepType?: number
  rollStatus?: number
  dispositionAction?: 'DIRECT_SHIP' | 'CANCEL' | 'SPLIT_TO_ORDER'
  isChecked?: number
  checkUser?: string
  checkTime?: string
  remark?: string
  steps?: ProcessStep[]
  stageOutputs?: StageOutputVO[]
  rewindParams?: RewindParamVO[]
  finishes?: FinishProductionVO[]
}

export interface WorkshopInstructionVO {
  sourceRows: number[]
  sourceWidthMm?: number
  sourcePieceCount: number
  instruction: string
  text: string
}

/** 加工单详情返回体，与后端 ProcessOrderDetailVO 对应。 */
export type ProcessOrderPrintStage =
  | 'DRAFT'
  | 'PENDING_ISSUE'
  | 'PENDING_MANUAL_CONFIRM'
  | 'WAITING_BACK_RECORD'
  | 'COMPLETED'
  | 'SETTLED'
  | 'VOIDED'
  | 'UNKNOWN'

export interface ProcessOrderDetailVO {
  order: ProcessOrder
  /** 服务端根据状态机解析出的稳定打印/生产阶段。 */
  printStage?: ProcessOrderPrintStage
  originalRolls: OriginalRoll[]
  rolls: OriginalRoll[]
  finishRolls: FinishRoll[]
  steps: ProcessStep[]
  rollProductions?: RollProductionVO[]
  workshopInstructions?: WorkshopInstructionVO[]
}

export type PrintViewVersion = 'ISSUED' | 'FINISHED'
export type PrintViewSource = 'LIVE_PREVIEW' | 'SNAPSHOT' | 'LEGACY_FALLBACK'

export interface ProcessOrderPrintViewVO {
  version: PrintViewVersion
  availableVersions: PrintViewVersion[]
  source: PrintViewSource
  schemaVersion?: string
  snapshotTime?: string
  snapshotUser?: string
  warning?: string
  historical?: boolean
  historicalIssueVersion?: number
  detail: ProcessOrderDetailVO
}
