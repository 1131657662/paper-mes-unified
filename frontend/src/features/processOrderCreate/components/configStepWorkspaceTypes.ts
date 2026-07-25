import type { CustomerProcessPrice } from '../../../types/customer'
import type { Machine } from '../../../types/machine'
import type {
  PlanPreviewVO,
  ProcessPlanDTO,
  ProcessRoutePreviewVO,
  ProcessStep,
} from '../../../types/processOrder'
import type { DefaultPlanOptions } from '../draftMappers'
import type { calculateRollWeightBalance } from '../weightBalanceModel'
import type { mergedSourceLocks } from '../rewindConsumptionUtils'
import type { RollDraft } from '../types'

export interface ConfigStepWorkspaceData {
  allSteps: ProcessStep[]
  balance?: ReturnType<typeof calculateRollWeightBalance>
  checkedIds: string[]
  configuredPlanIds: string[]
  customerPrices?: CustomerProcessPrice[]
  defaultSpareCount: number
  detailError: boolean
  detailLoading: boolean
  lockedRolls: ReturnType<typeof mergedSourceLocks>
  machines: Machine[]
  mainBatchOnlyCurrent: boolean
  mainBatchTargetCount: number
  orderUuid?: string
  plan?: ProcessPlanDTO
  planDefaults: DefaultPlanOptions
  previews: Record<string, PlanPreviewVO>
  roll?: RollDraft
  rolls: RollDraft[]
  routePreview?: ProcessRoutePreviewVO
  routePreviews: Record<string, ProcessRoutePreviewVO>
  saving: boolean
  selectedServiceRolls: RollDraft[]
  serviceConfigured: Record<string, boolean>
  serviceOnly: boolean
}

export interface ConfigStepWorkspaceActions {
  onApplyChecked: () => Promise<void>
  onClearSelection: () => void
  onNext: () => void
  onOpenRouteDesigner: (roll: RollDraft) => void
  onPlanChange: (plan: ProcessPlanDTO) => void
  onPrev: () => void
  onPreviewCurrent: () => Promise<void>
  onRetryDetail: () => void
  onSaveCurrent: () => Promise<void>
  onSelect: (localId: string) => void
  onSelectSameSpec: () => void
  onServiceDirtyChange: (dirty: boolean) => void
  onSynchronizeVersion: () => Promise<void>
  onToggle: (localId: string, checked: boolean) => void
}
