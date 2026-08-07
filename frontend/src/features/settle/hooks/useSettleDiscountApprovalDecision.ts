import { useMutation, useQueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'
import { settleService } from '../services/settleService'

export function useSettleDiscountApprovalDecision() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: settleService.decideDiscountApproval,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queries.settle.discountApprovalPage._def })
      void queryClient.invalidateQueries({ queryKey: queries.settle.latestDiscountApproval._def })
      void queryClient.invalidateQueries({ queryKey: queries.settle.discountApprovals._def })
      void queryClient.invalidateQueries({ queryKey: queries.settle.discountApprovalDetail._def })
      void queryClient.invalidateQueries({ queryKey: queries.notifications._def })
    },
  })
}
