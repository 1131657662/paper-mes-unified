import { useMutation } from '@tanstack/react-query'
import { createOrderService } from '../services/createOrderService'

export function useSavePlan() {
  return useMutation({ mutationFn: createOrderService.savePlan })
}
