import type { FinishProductionVO, ProcessStep, RewindParamVO, RollProductionVO } from '../../../types/processOrder'

/** 成品按门幅分组 */
export interface FinishGroup {
  width: number
  count: number
  totalEstimate: number
}

/** 合并组（mode 5 跨卷合并复卷） */
export interface MergeGroup {
  mainUuid: string
  sourceUuids: string[]
  allUuids: string[]
}

/** 加工步骤描述 */
export interface StepDesc {
  sort: number
  label: string
  isMain: boolean
  stepType: number
  modeLabel?: string
  details: string[]
}

/** 单行展示数据 */
export interface DisplayRow {
  key: string
  seq: number
  /** 单卷原纸标签，合并组为 "合并复卷 (N卷)" */
  label: string
  isMergeGroup: boolean
  originalUuids: string[]
  /** 参与该行的所有 RollProductionVO（合并组有多个） */
  rollProductions: RollProductionVO[]
  mainProduction: RollProductionVO
  sourceProductions: RollProductionVO[]
  steps: ProcessStep[]
  rewindParams: RewindParamVO[]
  /** 去重后的成品列表 */
  finishes: FinishProductionVO[]
  totalKnifeCount: number
  totalEstimateWeight: number
  rewindMode?: number
  isDirectShip: boolean
  hasConfig: boolean
}
