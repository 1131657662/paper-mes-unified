import { useMutation, useQueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'
import { deliveryService } from '../services/deliveryService'

export function useConfirmDelivery() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: deliveryService.confirm,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queries.delivery._def })
    },
  })
}
