import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deliveryService } from '../services/deliveryService'
import { invalidateDeliveryReadModels } from './invalidateDeliveryReadModels'

export function useCancelPendingDelivery() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: deliveryService.cancelPending,
    onSuccess: () => invalidateDeliveryReadModels(queryClient),
  })
}
