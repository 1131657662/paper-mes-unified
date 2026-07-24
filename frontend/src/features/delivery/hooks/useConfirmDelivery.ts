import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deliveryService } from '../services/deliveryService'
import { invalidateDeliveryReadModels } from './invalidateDeliveryReadModels'

export function useConfirmDelivery() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: deliveryService.confirm,
    onSuccess: () => invalidateDeliveryReadModels(queryClient),
  })
}
