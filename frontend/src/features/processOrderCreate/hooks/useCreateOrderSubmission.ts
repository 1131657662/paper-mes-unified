import { message } from 'antd'
import { clearCreateOrderLocalDraft } from '../localDraftStorage'
import type { CreateOrderDraftState } from './useCreateOrderDraftState'
import { useSubmitDraft } from './useSubmitDraft'

export function useCreateOrderSubmission(state: CreateOrderDraftState) {
  const { mutateAsync: submitDraft, isPending: submitting } = useSubmitDraft()

  const handleSubmit = async () => {
    if (!state.orderUuid) return false
    const result = await submitDraft({
      uuid: state.orderUuid,
      expectedVersion: state.draftVersion,
    })
    clearCreateOrderLocalDraft()
    state.setSubmitResult(result)
    message.success(`加工单 ${result.orderNo} 已提交`)
    return true
  }

  return { handleSubmit, submitting }
}
