import { useMutation, useQueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'
import { createOrderService } from '../services/createOrderService'

export function useSubmitDraft() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createOrderService.submit,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queries.createOrder.drafts.queryKey })
    },
  })
}
