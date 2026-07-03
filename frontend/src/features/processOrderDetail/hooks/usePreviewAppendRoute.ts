import { useMutation } from '@tanstack/react-query'
import { previewAppendProcessRoute } from '../../../api/processOrder'
import type { ProcessRoutePreviewDTO } from '../../../types/processOrder'

interface PreviewAppendRouteParams {
  orderUuid: string
  request: ProcessRoutePreviewDTO
}

export function usePreviewAppendRoute() {
  return useMutation({
    mutationFn: ({ orderUuid, request }: PreviewAppendRouteParams) =>
      previewAppendProcessRoute(orderUuid, request),
  })
}
