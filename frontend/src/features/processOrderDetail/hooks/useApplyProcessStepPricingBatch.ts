import { useMutation, useQueryClient } from '@tanstack/react-query'
import { applyProcessStepPricingBatch, type ProcessStepPricingBatchDTO } from '../../../api/processOrder'
import { queries } from '../../../queries'

interface ApplyPricingBatchParams {
  orderUuid: string
  values: ProcessStepPricingBatchDTO
}

export function useApplyProcessStepPricingBatch() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ orderUuid, values }: ApplyPricingBatchParams) => applyProcessStepPricingBatch(orderUuid, values),
    onSuccess: async (_, variables) => {
      await queryClient.invalidateQueries({
        queryKey: queries.processOrderDetail.detail(variables.orderUuid).queryKey,
      })
    },
  })
}
