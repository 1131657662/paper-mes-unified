import { useMutation, useQueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'
import { deliveryService } from '../services/deliveryService'

export function useCancelPendingDelivery() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: deliveryService.cancelPending,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queries.delivery._def })
    },
  })
}
