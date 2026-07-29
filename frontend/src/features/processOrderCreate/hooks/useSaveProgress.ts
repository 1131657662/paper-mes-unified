import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createOrderService } from '../services/createOrderService'
import { progressMatches, readLatestDraft, runReconciledDraftWrite } from '../draftWriteReconciliation'
import { invalidateCreateOrderDraft } from './invalidateCreateOrderDraft'

export function useSaveProgress() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (variables: Parameters<typeof createOrderService.saveProgress>[0]) => runReconciledDraftWrite({
      expectedVersion: variables.expectedVersion,
      isApplied: (draft) => progressMatches(draft, variables.currentStep),
      readLatest: () => readLatestDraft(queryClient, variables.uuid),
      recoverData: () => undefined,
      write: () => createOrderService.saveProgress(variables),
    }),
    onSuccess: (_, variables) => invalidateCreateOrderDraft(queryClient, variables.uuid),
  })
}
