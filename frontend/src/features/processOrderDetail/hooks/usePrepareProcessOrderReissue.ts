import { useMutation, useQueryClient } from '@tanstack/react-query'
import { prepareProcessOrderReissue } from '../../../api/processOrder'
import { invalidateProcessOrderReadModels } from './invalidateProcessOrderReadModels'

interface PrepareReissueParams {
  orderUuid: string
  requestId: string
  expectedVersion: number
  reason: string
}

export function usePrepareProcessOrderReissue() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ orderUuid, requestId, expectedVersion, reason }: PrepareReissueParams) =>
      prepareProcessOrderReissue(orderUuid, { requestId, expectedVersion, reason }),
    onSuccess: async (_, variables) => {
      await invalidateProcessOrderReadModels(queryClient, variables.orderUuid)
    },
  })
}
