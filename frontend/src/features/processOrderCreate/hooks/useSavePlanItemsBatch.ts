import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createOrderService } from '../services/createOrderService'
import {
  itemPlansMatch,
  readLatestDraft,
  recoverPlanPreviews,
  runReconciledDraftWrite,
} from '../draftWriteReconciliation'
import { invalidateCreateOrderDraft } from './invalidateCreateOrderDraft'

export function useSavePlanItemsBatch() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (variables: Parameters<typeof createOrderService.savePlanItemsBatch>[0]) => runReconciledDraftWrite({
      expectedVersion: variables.dto.expectedVersion,
      isApplied: (draft) => itemPlansMatch(draft, variables.dto),
      readLatest: () => readLatestDraft(queryClient, variables.orderUuid),
      recoverData: (draft) => recoverPlanPreviews(
        draft,
        variables.dto.items.map((item) => item.originalUuid),
      ),
      write: () => createOrderService.savePlanItemsBatch(variables),
    }),
    onSuccess: (_, variables) => invalidateCreateOrderDraft(queryClient, variables.orderUuid),
  })
}
