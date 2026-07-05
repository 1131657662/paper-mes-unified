import { useMutation } from '@tanstack/react-query'
import { createOrderService } from '../services/createOrderService'

export function usePreviewRoute() {
  return useMutation({ mutationFn: createOrderService.previewRoute })
}
