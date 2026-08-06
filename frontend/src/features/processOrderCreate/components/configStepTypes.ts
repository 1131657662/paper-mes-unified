import type { CustomerProcessPrice } from '../../../types/customer'
import type { Machine } from '../../../types/machine'
import type { PlanPreviewVO, ProcessPlanDTO, ProcessRoutePreviewVO, ProcessStep } from '../../../types/processOrder'
import type { DefaultPlanOptions } from '../draftMappers'
import type { RollDraft } from '../types'
import type { PlanBatchSaveResult, PlanSaveResult } from '../planSaveResult'

export type ConfigOperation = 'saving' | 'validating'

export interface ConfigStepProps {
  autoFinishConfigEnabled: boolean
  configuredPlanIds: string[]
  customerPrices?: CustomerProcessPrice[]
  defaultPlanOptions?: DefaultPlanOptions
  defaultSpareCount?: number
  machines: Machine[]
  draftVersion: number
  operation?: ConfigOperation
  onNext: () => void
  onOpenRouteDesigner: (roll: RollDraft) => void
  onPlanChange: (localId: string, plan: ProcessPlanDTO) => void
  onPendingChange: (pending: boolean) => void
  onPrev: () => void
  onPreviewPlan: (roll: RollDraft, plan: ProcessPlanDTO, signal?: AbortSignal) => Promise<void>
  onSavePlan: (roll: RollDraft, plan: ProcessPlanDTO) => Promise<PlanSaveResult>
  onSavePlanBatch: (rolls: RollDraft[], plan: ProcessPlanDTO) => Promise<PlanBatchSaveResult | false>
  onSelect: (localId: string) => void
  onServiceDirtyChange: (dirty: boolean) => void
  onDraftVersionChange: (version: number) => void
  orderUuid?: string
  plans: Record<string, ProcessPlanDTO>
  previews: Record<string, PlanPreviewVO>
  rolls: RollDraft[]
  routePreviews: Record<string, ProcessRoutePreviewVO>
  saving: boolean
  selectedId?: string
  serviceStepsByRoll?: Record<string, ProcessStep[]>
  onServiceStepsChange?: (changes: Record<string, ProcessStep[]>) => Promise<void>
}
