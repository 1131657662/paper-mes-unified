import { useQueryClient } from '@tanstack/react-query'
import { applyProcessAiConfirmation } from '../../processAi/applyProcessAiConfirmation'
import { packagingDraftFromCandidate } from '../../processAi/applyProcessAiConfirmation'
import { useDismissProcessAiPackaging } from '../../processAi/hooks/useDismissProcessAiPackaging'
import { usePendingProcessAiPackaging } from '../../processAi/hooks/usePendingProcessAiPackaging'
import { queries } from '../../../queries'
import type {
  ProcessAiConfirmResponse,
  ProcessAiPackagingDraft,
} from '../../processAi/types'
import type { CreateOrderDraftState } from './useCreateOrderDraftState'

export function useCreateOrderAiConfirmation(
  state: CreateOrderDraftState,
  onApplied?: () => Promise<unknown>,
) {
  const queryClient = useQueryClient()
  const pendingQuery = usePendingProcessAiPackaging(state.orderUuid
    ? { orderUuid: state.orderUuid, expectedVersion: state.draftVersion }
    : undefined)
  const dismissMutation = useDismissProcessAiPackaging()
  const packagingDrafts = Object.fromEntries(
    (pendingQuery.data ?? []).map((item) => [
      item.candidate.originalUuid,
      packagingDraftFromCandidate(item.parseId, item.candidate),
    ]),
  )

  const apply = (confirmation: ProcessAiConfirmResponse) => {
    const currentVersion = state.getDraftVersion()
    if (confirmation.expectedVersion !== currentVersion
      || confirmation.nextVersion !== currentVersion + 1) {
      throw new Error('AI 确认结果已过期，请刷新当前草稿后重试')
    }
    const applied = applyProcessAiConfirmation({
      confirmation,
      configuredPlanIds: state.configuredPlanIds,
      plans: state.plans,
      previews: state.previews,
      rolls: state.rolls,
    })
    applyPlans(state, applied)
    if (state.baseInfo && confirmation.remarkLong !== undefined) {
      state.setBaseInfo({
        ...state.baseInfo,
        remarkLong: confirmation.remarkLong,
      })
    }
    state.setDraftVersion(confirmation.nextVersion)
    void onApplied?.()
    return applied.updatedLocalIds
  }

  const consumePackagingDraft = (_originalUuid: string) => {
    void queryClient.invalidateQueries({
      queryKey: queries.processAi.pendingPackaging._def,
    })
  }

  const dismissPackagingDraft = async (draft: ProcessAiPackagingDraft) => {
    if (!state.orderUuid) return
    await dismissMutation.mutateAsync({
      orderUuid: state.orderUuid,
      expectedVersion: state.draftVersion,
      parseId: draft.parseId,
      ownerRollRef: draft.ownerRollRef,
    })
  }

  return {
    aiPackagingLoading: pendingQuery.isLoading,
    apply,
    consumePackagingDraft,
    dismissPackagingDraft,
    packagingDrafts,
  }
}

function applyPlans(
  state: CreateOrderDraftState,
  applied: ReturnType<typeof applyProcessAiConfirmation>,
) {
  state.setRolls(applied.rolls)
  state.setPlans(applied.plans)
  state.setPreviews(applied.previews)
  state.setConfiguredPlanIds(applied.configuredPlanIds)
}

export function mergeProcessAiPackagingDrafts(
  current: Record<string, ProcessAiPackagingDraft>,
  drafts: ProcessAiPackagingDraft[],
): Record<string, ProcessAiPackagingDraft> {
  const next = { ...current }
  for (const draft of drafts) next[draft.values.originalUuid] = draft
  return next
}

export function consumeProcessAiPackagingDraft(
  current: Record<string, ProcessAiPackagingDraft>,
  originalUuid: string,
): Record<string, ProcessAiPackagingDraft> {
  return Object.fromEntries(
    Object.entries(current).filter(([key]) => key !== originalUuid),
  )
}
