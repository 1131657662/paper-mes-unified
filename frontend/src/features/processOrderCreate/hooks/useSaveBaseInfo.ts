import { useMutation } from '@tanstack/react-query'
import { createOrderService } from '../services/createOrderService'

export function useSaveBaseInfo() {
  return useMutation({ mutationFn: createOrderService.saveBaseInfo })
}
