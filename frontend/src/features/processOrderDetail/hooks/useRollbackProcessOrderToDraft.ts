import { useMutation, useQueryClient } from '@tanstack/react-query'
import { rollbackProcessOrderToDraft } from '../../../api/processOrder'
import { invalidateProcessOrderReadModels } from './invalidateProcessOrderReadModels'

interface RollbackDraftParams {
  orderUuid: string
  reason: string
}

export function useRollbackProcessOrderToDraft() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ orderUuid, reason }: RollbackDraftParams) =>
      rollbackProcessOrderToDraft(orderUuid, { reason }),
    onSuccess: async (_, variables) => {
      await invalidateProcessOrderReadModels(queryClient, variables.orderUuid)
    },
  })
}
