import { useMutation, useQueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'
import { deliveryService } from '../services/deliveryService'

export function useRemoveDeliveryDetail() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: deliveryService.removeDetail,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queries.delivery._def })
      queryClient.invalidateQueries({ queryKey: queries.deliveryCustomerSpec._def })
    },
  })
}
