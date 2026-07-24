import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createOrderService } from '../services/createOrderService'
import { invalidateCreateOrderDraft } from './invalidateCreateOrderDraft'

export function useSavePlanBatch() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createOrderService.savePlanBatch,
    onSuccess: (_, variables) => invalidateCreateOrderDraft(queryClient, variables.orderUuid),
  })
}
