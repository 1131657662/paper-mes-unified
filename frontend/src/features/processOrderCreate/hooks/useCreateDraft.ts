import { useMutation } from '@tanstack/react-query'
import { createOrderService } from '../services/createOrderService'

export function useCreateDraft() {
  return useMutation({ mutationFn: createOrderService.createDraft })
}
