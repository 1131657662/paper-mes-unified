import { useMutation } from '@tanstack/react-query'
import { createOrderService } from '../services/createOrderService'

export function useSaveConfig() {
  return useMutation({ mutationFn: createOrderService.saveConfig })
}
