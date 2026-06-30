import { useMutation, useQueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'
import { deliveryService } from '../services/deliveryService'

export function useRollbackDelivery() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: deliveryService.rollback,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queries.delivery._def })
    },
  })
}
