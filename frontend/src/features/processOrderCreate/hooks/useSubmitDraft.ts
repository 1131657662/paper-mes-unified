import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createOrderService } from '../services/createOrderService'
import { invalidateSubmittedProcessOrder } from './invalidateCreateOrderDraft'

export function useSubmitDraft() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createOrderService.submit,
    onSuccess: () => invalidateSubmittedProcessOrder(queryClient),
  })
}
