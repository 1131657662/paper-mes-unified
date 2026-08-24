import type {
  ProcessAiConfirmResponse,
} from '../../processAi/types'
import type { CreateOrderDraftState } from './useCreateOrderDraftState'

export function useCreateOrderAiConfirmation(
  state: CreateOrderDraftState,
  onApplied?: () => Promise<unknown>,
) {
  const apply = async (confirmation: ProcessAiConfirmResponse) => {
    const currentVersion = state.getDraftVersion()
    if (!aiConfirmationVersionMatches(confirmation, currentVersion)) {
      throw new Error('AI 确认结果已过期，请刷新当前草稿后重试')
    }
    await onApplied?.()
  }

  return {
    apply,
  }
}

export function aiConfirmationVersionMatches(
  confirmation: Pick<ProcessAiConfirmResponse, 'expectedVersion' | 'nextVersion'>,
  currentVersion: number,
) {
  return confirmation.expectedVersion === currentVersion
    && confirmation.nextVersion === currentVersion + 1
}
