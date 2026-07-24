import { useMutation, useQueryClient } from '@tanstack/react-query'
import { applyProcessStepPricingBatch, type ProcessStepPricingBatchDTO } from '../../../api/processOrder'
import { invalidateProcessOrderReadModels } from './invalidateProcessOrderReadModels'

interface ApplyPricingBatchParams {
  orderUuid: string
  values: ProcessStepPricingBatchDTO
}

export function useApplyProcessStepPricingBatch() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ orderUuid, values }: ApplyPricingBatchParams) => applyProcessStepPricingBatch(orderUuid, values),
    onSuccess: async (_, variables) => {
      await invalidateProcessOrderReadModels(queryClient, variables.orderUuid)
    },
  })
}
