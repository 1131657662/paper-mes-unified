import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createOrderService } from '../services/createOrderService'
import {
  readLatestDraft,
  recoverPlanPreviews,
  runReconciledDraftWrite,
  singlePlanMatches,
} from '../draftWriteReconciliation'
import { invalidateCreateOrderDraft } from './invalidateCreateOrderDraft'

export function useSavePlan() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (variables: Parameters<typeof createOrderService.savePlan>[0]) => runReconciledDraftWrite({
      expectedVersion: variables.request.expectedVersion,
      isApplied: (draft) => singlePlanMatches(draft, variables.request),
      readLatest: () => readLatestDraft(queryClient, variables.orderUuid),
      recoverData: (draft) => recoverPlanPreviews(draft, [variables.rollUuid])[0]!,
      write: () => createOrderService.savePlan(variables),
    }),
    onSuccess: (_, variables) => invalidateCreateOrderDraft(queryClient, variables.orderUuid),
  })
}
