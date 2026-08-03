import { useMutation, useQueryClient } from '@tanstack/react-query'
import { prepareProcessOrderReissue } from '../../../api/processOrder'
import { invalidateProcessOrderReadModels } from './invalidateProcessOrderReadModels'

interface PrepareReissueParams {
  orderUuid: string
  expectedVersion: number
  reason: string
}

export function usePrepareProcessOrderReissue() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ orderUuid, expectedVersion, reason }: PrepareReissueParams) =>
      prepareProcessOrderReissue(orderUuid, { expectedVersion, reason }),
    onSuccess: async (_, variables) => {
      await invalidateProcessOrderReadModels(queryClient, variables.orderUuid)
    },
  })
}
