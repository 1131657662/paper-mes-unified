import { useMutation, useQueryClient } from '@tanstack/react-query'
import { settleService } from '../services/settleService'
import { invalidateSettleFinancialChange } from '../queries/invalidateSettleFinancialChange'

export function useCancelReceive() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: settleService.cancelReceive,
    onSettled: (_, __, variables) => invalidateSettleFinancialChange(queryClient, variables.uuid),
  })
}
