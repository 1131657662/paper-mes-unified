import { useMutation, useQueryClient } from '@tanstack/react-query'
import { saveAppendProcessRoute } from '../../../api/processOrder'
import { queries } from '../../../queries'
import type { ProcessRoutePreviewDTO } from '../../../types/processOrder'

interface SaveAppendRouteParams {
  orderUuid: string
  request: ProcessRoutePreviewDTO
}

export function useSaveAppendRoute() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ orderUuid, request }: SaveAppendRouteParams) =>
      saveAppendProcessRoute(orderUuid, request),
    onSuccess: async (_, variables) => {
      await queryClient.invalidateQueries({
        queryKey: queries.processOrderDetail.detail(variables.orderUuid).queryKey,
      })
    },
  })
}
