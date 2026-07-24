import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deliveryService } from '../services/deliveryService'
import { invalidateDeliveryReadModels } from './invalidateDeliveryReadModels'

export function useAppendDeliveryDetails() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: deliveryService.appendDetails,
    onSuccess: () => invalidateDeliveryReadModels(queryClient),
  })
}
