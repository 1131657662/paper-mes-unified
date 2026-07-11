import { useMutation, useQueryClient } from '@tanstack/react-query'
import { rollbackProcessOrderToDraft } from '../../../api/processOrder'
import { queries } from '../../../queries'

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
      await queryClient.invalidateQueries({
        queryKey: queries.processOrderDetail.detail(variables.orderUuid).queryKey,
      })
    },
  })
}
