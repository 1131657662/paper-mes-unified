import { useMutation } from '@tanstack/react-query'
import { deliveryInventoryService } from '../services/deliveryInventoryService'

export function useValidateDeliveryInventory() {
  return useMutation({ mutationFn: deliveryInventoryService.validateAvailability })
}
