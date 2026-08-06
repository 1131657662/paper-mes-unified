import type { CustomerProcessPrice } from '../../../types/customer'
import type { Machine } from '../../../types/machine'
import type {
  PlanPreviewVO,
  ProcessPlanDTO,
  ProcessRoutePreviewVO,
  ProcessStep,
} from '../../../types/processOrder'
import type { calculateRollWeightBalance } from '../weightBalanceModel'
import type { mergedSourceLocks } from '../rewindConsumptionUtils'
import type { RollDraft } from '../types'
import type { ConfigOperation } from './configStepTypes'

export type ConfigEditorTab = 'plan' | 'service'

export interface ConfigStepWorkspaceData {
  activeEditor: ConfigEditorTab
  allSteps: ProcessStep[]
  autoFinishConfigEnabled: boolean
  balance?: ReturnType<typeof calculateRollWeightBalance>
  checkedIds: string[]
  configuredPlanIds: string[]
  customerPrices?: CustomerProcessPrice[]
  detailError: boolean
  detailLoading: boolean
  draftVersion: number
  lockedRolls: ReturnType<typeof mergedSourceLocks>
  machines: Machine[]
  mainBatchTargetCount: number
  orderUuid?: string
  operation?: ConfigOperation
  plan?: ProcessPlanDTO
  plans: Record<string, ProcessPlanDTO>
  previewError?: string
  previewing: boolean
  previews: Record<string, PlanPreviewVO>
  roll?: RollDraft
  rolls: RollDraft[]
  routePreview?: ProcessRoutePreviewVO
  routePreviews: Record<string, ProcessRoutePreviewVO>
  saving: boolean
  selectionDisabledReasons: Record<string, string>
  selectedServiceRolls: RollDraft[]
  serviceConfigured: Record<string, boolean>
  serviceOnly: boolean
  serviceStepsByRoll: Record<string, ProcessStep[]>
  onServiceStepsChange?: (changes: Record<string, ProcessStep[]>) => Promise<void>
}

export interface ConfigStepWorkspaceActions {
  onApplyChecked: () => Promise<void>
  onClearSelection: () => void
  onCurrentServiceSaved: () => void
  onEditorChange: (tab: ConfigEditorTab) => void
  onNext: () => void
  onOpenRouteDesigner: (roll: RollDraft) => void
  onPlanChange: (plan: ProcessPlanDTO) => void
  onPrev: () => void
  onPreviewCurrent: () => Promise<void>
  onRetryDetail: () => void
  onSaveCurrent: () => Promise<void>
  onServiceBatchApplied: () => void
  onServicePendingChange: (pending: boolean) => void
  onSelect: (localId: string) => void
  onSelectSameSpec: () => void
  onServiceDirtyChange: (dirty: boolean) => void
  onSynchronizeVersion: () => Promise<number>
  onToggle: (localId: string, checked: boolean) => void
}
