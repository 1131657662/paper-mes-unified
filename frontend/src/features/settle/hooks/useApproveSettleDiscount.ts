import { useMutation, useQueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'
import { settleService } from '../services/settleService'

export function useApproveSettleDiscount() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: settleService.approveDiscount,
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: queries.settle.discountApprovals(variables.uuid).queryKey })
      queryClient.invalidateQueries({ queryKey: queries.settle.detail(variables.uuid).queryKey })
    },
  })
}
