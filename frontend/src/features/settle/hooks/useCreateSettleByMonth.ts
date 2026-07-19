import { useMutation, useQueryClient } from '@tanstack/react-query'
import { settleService } from '../services/settleService'
import { invalidateSettleAfterCreate } from '../queries/invalidateSettleAfterCreate'

export function useCreateSettleByMonth() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: settleService.createByMonth,
    onSuccess: () => invalidateSettleAfterCreate(queryClient),
  })
}
