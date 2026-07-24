import type { ServiceApplyTargets } from './serviceStepBatchModel'

export interface ServiceEditorStatus {
  analysis: ServiceApplyTargets
  dirty: boolean
  previousSummary?: string
  summary: string
}
