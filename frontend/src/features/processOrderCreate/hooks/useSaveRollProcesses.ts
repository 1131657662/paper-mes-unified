import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createOrderService } from '../services/createOrderService'
import { invalidateCreateOrderDraft } from './invalidateCreateOrderDraft'

export function useSaveRollProcesses() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createOrderService.saveRollProcesses,
    onSuccess: (_, variables) => invalidateCreateOrderDraft(queryClient, variables.orderUuid),
  })
}
