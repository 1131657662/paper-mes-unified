import type { ProcessPlanDTO } from '../../types/processOrder'

export const STEP_TYPE_SAW = 1
export const STEP_TYPE_REWIND = 2

export interface DetailRouteFormState {
  baseStageLevel: number
  firstOutputs: DetailRouteOutputRow[]
  firstSawKnifeCount?: number
  firstStepType: number
  firstUnitPrice?: number
  preferredSourceOutputKey?: string
  stages: DetailRouteStageForm[]
}

export interface DetailRouteStageForm {
  id: string
  inputOutputKeys: string[]
  plan: ProcessPlanDTO
  stageLevel: number
  stepType: number
}

export interface DetailRouteOutputRow {
  estimateWeight: number
  finishCoreDiameter?: number
  finishDiameter?: number
  finishRollNo?: string
  finishWidth: number
  gramWeight?: number
  isRemain?: number
  label: string
  outputKey: string
  parentOutputKey?: string
  parentOutputUuid?: string
  paperName?: string
  sourceStepType?: number
  sourceOutputKey?: string
  sourceRollNo?: string
  sourceSummary?: string
  stageLevel: number
}

export interface DetailRoutePriceDefaults {
  rewindUnitPrice?: number
  sawUnitPrice?: number
}

export function routeStepName(stepType?: number): string {
  if (stepType === STEP_TYPE_SAW) return '锯纸'
  if (stepType === STEP_TYPE_REWIND) return '复卷'
  return '未配置'
}
