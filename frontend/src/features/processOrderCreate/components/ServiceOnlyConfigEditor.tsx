import type { CustomerProcessPrice } from '../../../types/customer'
import type { ProcessStep } from '../../../types/processOrder'
import type { RollDraft } from '../types'
import DraftAdditionalProcesses from './DraftAdditionalProcesses'
import type { ServiceEditorStatus } from '../serviceStepEditorTypes'
import type { ProcessAiPackagingDraft } from '../../processAi/types'

interface Props {
  aiPackagingDraft?: ProcessAiPackagingDraft
  allSteps: ProcessStep[]
  customerPrices?: CustomerProcessPrice[]
  detailError: boolean
  detailLoading: boolean
  draftVersion: number
  orderUuid?: string
  roll: RollDraft
  selectedRolls: RollDraft[]
  onBatchApplied: () => void
  onAiPackagingDraftConsumed?: (originalUuid: string) => void
  onAiPackagingDraftDismissed?: (draft: ProcessAiPackagingDraft) => Promise<void>
  onCurrentSaved: () => void
  onStatusChange: (status?: ServiceEditorStatus) => void
  onRetryDetail: () => void
  onSynchronizeVersion: () => Promise<number>
  onVersionSyncBlockedChange: (blocked: boolean) => void
  onWritePendingChange: (pending: boolean) => void
  versionSyncBlocked: boolean
}

export default function ServiceOnlyConfigEditor({
  aiPackagingDraft,
  allSteps,
  customerPrices,
  detailError,
  detailLoading,
  draftVersion,
  orderUuid,
  roll,
  selectedRolls,
  onAiPackagingDraftConsumed,
  onAiPackagingDraftDismissed,
  onBatchApplied,
  onCurrentSaved,
  onStatusChange,
  onRetryDetail,
  onSynchronizeVersion,
  onVersionSyncBlockedChange,
  onWritePendingChange,
  versionSyncBlocked,
}: Props) {
  return (
    <div className="service-only-config-editor">
      <DraftAdditionalProcesses
        aiPackagingDraft={aiPackagingDraft}
        key={roll.localId}
        allSteps={allSteps}
        orderUuid={orderUuid}
        roll={roll}
        selectedRolls={selectedRolls}
        onAiPackagingDraftConsumed={onAiPackagingDraftConsumed}
        onAiPackagingDraftDismissed={onAiPackagingDraftDismissed}
        onBatchApplied={onBatchApplied}
        onCurrentSaved={onCurrentSaved}
        customerPrices={customerPrices}
        detailError={detailError}
        detailLoading={detailLoading}
        draftVersion={draftVersion}
        onStatusChange={onStatusChange}
        onRetryDetail={onRetryDetail}
        onSynchronizeVersion={onSynchronizeVersion}
        onVersionSyncBlockedChange={onVersionSyncBlockedChange}
        onWritePendingChange={onWritePendingChange}
        versionSyncBlocked={versionSyncBlocked}
      />
    </div>
  )
}
