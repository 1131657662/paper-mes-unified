import { useMutation, useQueryClient } from '@tanstack/react-query'
import { saveAppendProcessRoute } from '../../../api/processOrder'
import type { ProcessRoutePreviewDTO } from '../../../types/processOrder'
import { invalidateProcessOrderReadModels } from './invalidateProcessOrderReadModels'

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
      await invalidateProcessOrderReadModels(queryClient, variables.orderUuid)
    },
  })
}
