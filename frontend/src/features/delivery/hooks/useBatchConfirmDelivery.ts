import { useMutation, useQueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'
import { deliveryService } from '../services/deliveryService'

export function useBatchConfirmDelivery() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: deliveryService.confirmBatch,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queries.delivery._def })
      queryClient.invalidateQueries({ queryKey: queries.deliveryCustomerSpec._def })
    },
  })
}
