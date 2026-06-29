import type {
  FinishRoll,
  OriginalRoll,
  RollProductionVO,
} from '../../../types/processOrder'

export type FinishBindMode = 'linked' | 'inferred' | 'pool'

export interface WorkbenchFinish {
  finish: FinishRoll
  bindMode: FinishBindMode
}

export interface BackRecordWorkItem {
  key: string
  kind: 'roll' | 'pool'
  title: string
  subtitle?: string
  roll?: OriginalRoll
  production?: RollProductionVO
  rollProductions: RollProductionVO[]
  isMergeGroup: boolean
  sourceMode: FinishBindMode | 'none'
  finishes: WorkbenchFinish[]
}

export interface BackRecordWorkbenchData {
  items: BackRecordWorkItem[]
}
