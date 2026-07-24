import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createOrderService } from '../services/createOrderService'
import { invalidateCreateOrderDraft } from './invalidateCreateOrderDraft'

export function useSaveBaseInfo() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createOrderService.saveBaseInfo,
    onSuccess: (_, variables) => invalidateCreateOrderDraft(queryClient, variables.uuid),
  })
}
