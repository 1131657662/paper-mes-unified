import { useMutation } from '@tanstack/react-query'
import { createOrderService } from '../services/createOrderService'

export function useReplaceRolls() {
  return useMutation({ mutationFn: createOrderService.replaceRolls })
}
