import { useMutation } from '@tanstack/react-query'
import { previewProcessStepPricingBatch, type ProcessStepPricingBatchDTO } from '../../../api/processOrder'

interface PreviewPricingBatchParams {
  orderUuid: string
  values: ProcessStepPricingBatchDTO
}

export function usePreviewProcessStepPricingBatch() {
  return useMutation({
    mutationFn: ({ orderUuid, values }: PreviewPricingBatchParams) => previewProcessStepPricingBatch(orderUuid, values),
  })
}
