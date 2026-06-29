import { useMutation } from '@tanstack/react-query'
import { createOrderService } from '../services/createOrderService'

export function useSavePlanBatch() {
  return useMutation({ mutationFn: createOrderService.savePlanBatch })
}
