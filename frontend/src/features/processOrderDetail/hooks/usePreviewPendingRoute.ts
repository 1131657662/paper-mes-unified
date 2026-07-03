import { useMutation } from '@tanstack/react-query'
import { previewPendingProcessRoute } from '../../../api/processOrder'
import type { ProcessRoutePreviewDTO } from '../../../types/processOrder'

interface PreviewPendingRouteParams {
  orderUuid: string
  request: ProcessRoutePreviewDTO
}

export function usePreviewPendingRoute() {
  return useMutation({
    mutationFn: ({ orderUuid, request }: PreviewPendingRouteParams) =>
      previewPendingProcessRoute(orderUuid, request),
  })
}
