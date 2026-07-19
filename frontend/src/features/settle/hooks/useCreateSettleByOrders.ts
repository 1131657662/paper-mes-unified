import { useMutation, useQueryClient } from '@tanstack/react-query'
import { settleService } from '../services/settleService'
import { invalidateSettleAfterCreate } from '../queries/invalidateSettleAfterCreate'

export function useCreateSettleByOrders() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: settleService.createByOrders,
    onSuccess: () => invalidateSettleAfterCreate(queryClient),
  })
}
