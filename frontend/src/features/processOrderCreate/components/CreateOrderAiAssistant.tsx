import ProcessAiAssistantEntry from '../../processAi/components/ProcessAiAssistantEntry'
import type { ProcessAiConfirmResponse } from '../../processAi/types'
import type { useCreateOrderPage } from '../hooks/useCreateOrderPage'

type AssistantState = Pick<ReturnType<typeof useCreateOrderPage>,
  'applyAiConfirmation' | 'baseInfo' | 'draftVersion' | 'orderUuid' | 'plans' | 'rolls' | 'selectedId'>

interface Props {
  currentStep: 3 | 4
  state: AssistantState
}

export default function CreateOrderAiAssistant({ currentStep, state }: Props) {
  const apply = (confirmation: ProcessAiConfirmResponse) => {
    state.applyAiConfirmation(confirmation)
  }
  return <ProcessAiAssistantEntry currentStep={currentStep} draftVersion={state.draftVersion}
    orderUuid={state.orderUuid} plans={state.plans} remarkLong={state.baseInfo?.remarkLong}
    rolls={state.rolls} selectedRollId={state.selectedId} onApply={apply} />
}
