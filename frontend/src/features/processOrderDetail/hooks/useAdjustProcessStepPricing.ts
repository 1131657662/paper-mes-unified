import { useMutation, useQueryClient } from '@tanstack/react-query'
import { adjustProcessStepPricing, type ProcessStepPricingAdjustmentDTO } from '../../../api/processOrder'
import { queries } from '../../../queries'

interface AdjustPricingParams {
  orderUuid: string
  stepUuid: string
  values: ProcessStepPricingAdjustmentDTO
}

export function useAdjustProcessStepPricing() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ stepUuid, values }: AdjustPricingParams) => adjustProcessStepPricing(stepUuid, values),
    onSuccess: async (_, variables) => {
      await queryClient.invalidateQueries({
        queryKey: queries.processOrderDetail.detail(variables.orderUuid).queryKey,
      })
    },
  })
}
