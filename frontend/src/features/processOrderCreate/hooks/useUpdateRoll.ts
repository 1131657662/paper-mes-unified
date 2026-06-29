import { useMutation } from '@tanstack/react-query'
import { createOrderService } from '../services/createOrderService'

export function useUpdateRoll() {
  return useMutation({ mutationFn: createOrderService.updateRoll })
}
