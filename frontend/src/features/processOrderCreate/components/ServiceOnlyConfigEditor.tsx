import type { CustomerProcessPrice } from '../../../types/customer'
import type { ProcessStep } from '../../../types/processOrder'
import type { RollDraft } from '../types'
import DraftAdditionalProcesses from './DraftAdditionalProcesses'
import type { ServiceEditorStatus } from '../serviceStepEditorTypes'

interface Props {
  allSteps: ProcessStep[]
  customerPrices?: CustomerProcessPrice[]
  detailError: boolean
  detailLoading: boolean
  orderUuid?: string
  roll: RollDraft
  selectedRolls: RollDraft[]
  onStatusChange: (status?: ServiceEditorStatus) => void
  onRetryDetail: () => void
  onSynchronizeVersion: () => Promise<void>
  onVersionSyncBlockedChange: (blocked: boolean) => void
  onWritePendingChange: (pending: boolean) => void
  versionSyncBlocked: boolean
}

export default function ServiceOnlyConfigEditor({
  allSteps,
  customerPrices,
  detailError,
  detailLoading,
  orderUuid,
  roll,
  selectedRolls,
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
        key={roll.localId}
        allSteps={allSteps}
        orderUuid={orderUuid}
        roll={roll}
        selectedRolls={selectedRolls}
        customerPrices={customerPrices}
        detailError={detailError}
        detailLoading={detailLoading}
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
