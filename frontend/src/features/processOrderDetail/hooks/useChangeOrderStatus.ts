import { useMutation, useQueryClient } from '@tanstack/react-query'
import { changeOrderStatus } from '../../../api/processOrder'
import { invalidateProcessOrderReadModels } from './invalidateProcessOrderReadModels'

interface ChangeStatusParams {
  orderUuid: string
  targetStatus: number
  reason?: string
}

export function useChangeOrderStatus() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ orderUuid, reason, targetStatus }: ChangeStatusParams) =>
      changeOrderStatus(orderUuid, { reason, targetStatus }),
    onSuccess: async (_, variables) => {
      await invalidateProcessOrderReadModels(queryClient, variables.orderUuid)
    },
  })
}
