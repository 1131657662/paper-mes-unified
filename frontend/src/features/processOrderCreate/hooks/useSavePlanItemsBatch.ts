import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createOrderService } from '../services/createOrderService'
import { invalidateCreateOrderDraft } from './invalidateCreateOrderDraft'

export function useSavePlanItemsBatch() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createOrderService.savePlanItemsBatch,
    onSuccess: (_, variables) => invalidateCreateOrderDraft(queryClient, variables.orderUuid),
  })
}
