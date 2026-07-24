import { useMutation, useQueryClient } from '@tanstack/react-query'
import { completeProcessOrder } from '../../../api/processOrder'
import { invalidateProcessOrderReadModels } from './invalidateProcessOrderReadModels'

interface CompleteProcessingParams {
  orderUuid: string
  reason?: string
}

export function useCompleteProcessing() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ orderUuid, reason }: CompleteProcessingParams) =>
      completeProcessOrder(orderUuid, reason),
    onSuccess: async (_, variables) => {
      await invalidateProcessOrderReadModels(queryClient, variables.orderUuid)
    },
  })
}
