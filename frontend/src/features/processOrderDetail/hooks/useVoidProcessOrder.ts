import { useMutation, useQueryClient } from '@tanstack/react-query'
import { voidProcessOrder } from '../../../api/processOrder'
import { invalidateProcessOrderReadModels } from './invalidateProcessOrderReadModels'

interface VoidOrderParams {
  orderUuid: string
  reason: string
}

export function useVoidProcessOrder() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ orderUuid, reason }: VoidOrderParams) =>
      voidProcessOrder(orderUuid, { reason }),
    onSuccess: async (_, variables) => {
      await invalidateProcessOrderReadModels(queryClient, variables.orderUuid)
    },
  })
}
