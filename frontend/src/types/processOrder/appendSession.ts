import type { FinishConfigSaveDTO, OriginalRollDTO } from './finishConfig'
import type {
  PlanPreviewVO,
  ProcessPlanDTO,
  ProcessRoutePreviewDTO,
  ProcessRoutePreviewVO,
} from './planning'
import type { OriginalRoll } from './roll'

export interface ProcessOrderAppendRollProcessDTO {
  rollUuid: string
  processMode: number
  mainStepType?: number
  machineUuid?: string
}

export interface ProcessOrderAppendRollBatchDTO {
  expectedSessionVersion: number
  rolls: OriginalRollDTO[]
}

export interface ProcessOrderAppendSessionCreateDTO {
  expectedOrderVersion: number
  reason?: string
}

export interface ProcessOrderAppendProcessSettingsDTO {
  expectedSessionVersion: number
  rolls: ProcessOrderAppendRollProcessDTO[]
}

export interface ProcessOrderAppendPlanSaveDTO {
  expectedSessionVersion: number
  rollUuid: string
  config: FinishConfigSaveDTO
  configType?: 'singlePlan' | 'routePlan'
  previewJson?: string
}

export interface ProcessOrderAppendPlanPreviewDTO {
  expectedSessionVersion: number
  plan: ProcessPlanDTO
}

export interface ProcessOrderAppendPreviewDTO {
  expectedSessionVersion: number
}

export interface ProcessOrderAppendCommitDTO {
  expectedOrderVersion: number
  requestId: string
}

export interface ProcessOrderAppendRollVO extends OriginalRoll {
  configStatus?: number
  configType?: 'singlePlan' | 'routePlan' | string
  lastError?: string
  config?: FinishConfigSaveDTO
  previewJson?: string
  preview?: PlanPreviewVO
  route?: ProcessRoutePreviewDTO
  routePreview?: ProcessRoutePreviewVO
}

export interface ProcessOrderAppendSessionVO {
  sessionUuid: string
  orderUuid: string
  orderNo?: string
  baseOrderVersion?: number
  currentOrderVersion?: number
  sessionVersion?: number
  status?: 'DRAFT' | 'READY' | 'APPLIED' | 'CANCELLED' | 'EXPIRED' | string
  reason?: string
  rolls?: ProcessOrderAppendRollVO[]
}

export interface ProcessOrderAppendCommitResult {
  sessionUuid?: string
  orderUuid?: string
  orderVersion?: number
  rollUuids?: string[]
  finishRollNos?: string[]
}
