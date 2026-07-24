import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createOrderService } from '../services/createOrderService'
import { invalidateCreateOrderDraft } from './invalidateCreateOrderDraft'

export function useSaveProgress() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createOrderService.saveProgress,
    onSuccess: (_, variables) => invalidateCreateOrderDraft(queryClient, variables.uuid),
  })
}
