import type { CustomerProcessPrice } from '../../../types/customer'
import type { Machine } from '../../../types/machine'
import type { PlanPreviewVO, ProcessPlanDTO, ProcessRoutePreviewVO } from '../../../types/processOrder'
import type { DefaultPlanOptions } from '../draftMappers'
import type { RollDraft } from '../types'

export interface ConfigStepProps {
  configuredPlanIds: string[]
  customerPrices?: CustomerProcessPrice[]
  defaultPlanOptions?: DefaultPlanOptions
  defaultSpareCount?: number
  machines: Machine[]
  draftVersion: number
  onNext: () => void
  onOpenRouteDesigner: (roll: RollDraft) => void
  onPlanChange: (localId: string, plan: ProcessPlanDTO) => void
  onPrev: () => void
  onPreviewPlan: (roll: RollDraft, plan: ProcessPlanDTO) => Promise<void>
  onSavePlan: (roll: RollDraft, plan: ProcessPlanDTO) => Promise<void>
  onSavePlanBatch: (rolls: RollDraft[], plan: ProcessPlanDTO) => Promise<void>
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
}
