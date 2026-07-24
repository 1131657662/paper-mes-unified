import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createOrderService } from '../services/createOrderService'
import { invalidateCreateOrderDraft } from './invalidateCreateOrderDraft'

export function useReplaceRolls() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createOrderService.replaceRolls,
    onSuccess: (_, variables) => invalidateCreateOrderDraft(queryClient, variables.uuid),
  })
}
