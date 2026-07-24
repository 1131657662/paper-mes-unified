import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createOrderService } from '../services/createOrderService'
import { invalidateCreateOrderDraft } from './invalidateCreateOrderDraft'

export function useSaveConfig() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createOrderService.saveConfig,
    onSuccess: (_, variables) => invalidateCreateOrderDraft(queryClient, variables.orderUuid),
  })
}
