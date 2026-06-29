import { useMutation } from '@tanstack/react-query'
import { createOrderService } from '../services/createOrderService'

export function usePreviewPlan() {
  return useMutation({ mutationFn: createOrderService.previewPlan })
}
