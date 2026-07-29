import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deliveryService } from '../services/deliveryService'
import { invalidateDeliveryReadModels } from './invalidateDeliveryReadModels'

export function useUpdatePendingDelivery() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: deliveryService.updatePending,
    onSuccess: () => invalidateDeliveryReadModels(queryClient),
  })
}
