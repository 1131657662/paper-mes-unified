import { useMutation, useQueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'
import { remainService } from '../services/remainService'

export function useCancelRemainRefund() {
  const queryClient = useQueryClient()
  return useMutation({ mutationFn: remainService.cancelRefund, onSuccess: () => queryClient.invalidateQueries({ queryKey: queries.remain._def }) })
}
