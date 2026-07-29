import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createOrderService } from '../services/createOrderService'
import { readLatestDraft, rollProcessesMatch, runReconciledDraftWrite } from '../draftWriteReconciliation'
import { invalidateCreateOrderDraft } from './invalidateCreateOrderDraft'

export function useSaveRollProcesses() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (variables: Parameters<typeof createOrderService.saveRollProcesses>[0]) => runReconciledDraftWrite({
      expectedVersion: variables.dto.expectedVersion,
      isApplied: (draft) => rollProcessesMatch(draft, variables.dto),
      readLatest: () => readLatestDraft(queryClient, variables.orderUuid),
      recoverData: () => undefined,
      write: () => createOrderService.saveRollProcesses(variables),
    }),
    onSuccess: (_, variables) => invalidateCreateOrderDraft(queryClient, variables.orderUuid),
  })
}
