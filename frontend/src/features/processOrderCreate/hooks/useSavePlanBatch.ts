import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createOrderService } from '../services/createOrderService'
import {
  batchPlanMatches,
  readLatestDraft,
  recoverPlanPreviews,
  runReconciledDraftWrite,
} from '../draftWriteReconciliation'
import { invalidateCreateOrderDraft } from './invalidateCreateOrderDraft'

export function useSavePlanBatch() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (variables: Parameters<typeof createOrderService.savePlanBatch>[0]) => runReconciledDraftWrite({
      expectedVersion: variables.dto.expectedVersion,
      isApplied: (draft) => batchPlanMatches(draft, variables.dto),
      readLatest: () => readLatestDraft(queryClient, variables.orderUuid),
      recoverData: (draft) => recoverPlanPreviews(draft, variables.dto.originalUuids),
      write: () => createOrderService.savePlanBatch(variables),
    }),
    onSuccess: (_, variables) => invalidateCreateOrderDraft(queryClient, variables.orderUuid),
  })
}
