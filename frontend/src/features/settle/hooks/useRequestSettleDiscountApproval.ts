import { useMutation, useQueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'
import { settleService } from '../services/settleService'

export function useRequestSettleDiscountApproval() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: settleService.requestDiscountApproval,
    onSuccess: (_, variables) => queryClient.invalidateQueries({
      queryKey: queries.settle.discountApprovals(variables.uuid).queryKey,
    }),
  })
}
