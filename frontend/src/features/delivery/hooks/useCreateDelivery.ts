import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deliveryService } from '../services/deliveryService'
import { invalidateDeliveryReadModels } from './invalidateDeliveryReadModels'

export function useCreateDelivery() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: deliveryService.create,
    onSuccess: () => invalidateDeliveryReadModels(queryClient),
  })
}
