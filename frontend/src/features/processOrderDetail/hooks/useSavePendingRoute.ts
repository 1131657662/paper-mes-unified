import { useMutation, useQueryClient } from '@tanstack/react-query'
import { savePendingProcessRoute } from '../../../api/processOrder'
import type { ProcessRoutePreviewDTO } from '../../../types/processOrder'
import { invalidateProcessOrderReadModels } from './invalidateProcessOrderReadModels'

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
      await invalidateProcessOrderReadModels(queryClient, variables.orderUuid)
    },
  })
}
