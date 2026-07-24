import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deliveryService } from '../services/deliveryService'
import { invalidateDeliveryReadModels } from './invalidateDeliveryReadModels'

export function useRemoveDeliveryDetail() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: deliveryService.removeDetail,
    onSuccess: () => invalidateDeliveryReadModels(queryClient),
  })
}
