import { useMutation, useQueryClient } from '@tanstack/react-query'
import { savePendingProcessRoute } from '../../../api/processOrder'
import { queries } from '../../../queries'
import type { ProcessRoutePreviewDTO } from '../../../types/processOrder'

interface SavePendingRouteParams {
  orderUuid: string
  request: ProcessRoutePreviewDTO
}

export function useSavePendingRoute() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ orderUuid, request }: SavePendingRouteParams) =>
      savePendingProcessRoute(orderUuid, request),
    onSuccess: async (_, variables) => {
      await queryClient.invalidateQueries({
        queryKey: queries.processOrderDetail.detail(variables.orderUuid).queryKey,
      })
    },
  })
}
