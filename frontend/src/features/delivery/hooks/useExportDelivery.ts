import { useMutation } from '@tanstack/react-query'
import { deliveryService } from '../services/deliveryService'

export function useExportDelivery() {
  return useMutation({
    mutationFn: deliveryService.export,
  })
}
