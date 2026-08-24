import { useMutation, useQueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'
import { remainService } from '../services/remainService'

export function useBindRemainNextSettlement() {
  const queryClient = useQueryClient()
  return useMutation({ mutationFn: remainService.bindNextSettlement, onSuccess: () => queryClient.invalidateQueries({ queryKey: queries.remain._def }) })
}
