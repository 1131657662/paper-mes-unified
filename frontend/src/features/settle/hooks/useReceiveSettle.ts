import { useMutation, useQueryClient } from '@tanstack/react-query'
import { settleService } from '../services/settleService'
import { invalidateSettleFinancialChange } from '../queries/invalidateSettleFinancialChange'

export function useReceiveSettle() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: settleService.receive,
    onSuccess: (_, variables) => invalidateSettleFinancialChange(queryClient, variables.uuid),
  })
}
