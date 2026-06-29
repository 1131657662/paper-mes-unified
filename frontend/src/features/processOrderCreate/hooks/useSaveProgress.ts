import { useMutation } from '@tanstack/react-query'
import { createOrderService } from '../services/createOrderService'

export function useSaveProgress() {
  return useMutation({ mutationFn: createOrderService.saveProgress })
}
