import { useMutation, useQueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'
import { remainService } from '../services/remainService'

export function useRollbackRemain() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: remainService.rollback,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queries.remain._def }),
  })
}
