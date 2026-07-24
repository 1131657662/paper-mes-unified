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
  footerContainer?: HTMLElement | null
  onStatusChange: (status?: ServiceEditorStatus) => void
  onRetryDetail: () => void
}

export default function ServiceOnlyConfigEditor({
  allSteps,
  customerPrices,
  detailError,
  detailLoading,
  orderUuid,
  roll,
  selectedRolls,
  footerContainer,
  onStatusChange,
  onRetryDetail,
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
        footerContainer={footerContainer}
        onStatusChange={onStatusChange}
        onRetryDetail={onRetryDetail}
      />
    </div>
  )
}
