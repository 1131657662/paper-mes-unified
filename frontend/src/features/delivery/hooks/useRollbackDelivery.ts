import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deliveryService } from '../services/deliveryService'
import { invalidateDeliveryReadModels } from './invalidateDeliveryReadModels'

export function useRollbackDelivery() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: deliveryService.rollback,
    onSuccess: () => invalidateDeliveryReadModels(queryClient),
  })
}
