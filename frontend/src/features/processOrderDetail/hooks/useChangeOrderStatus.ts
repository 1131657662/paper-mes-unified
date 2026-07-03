import { useMutation, useQueryClient } from '@tanstack/react-query'
import { changeOrderStatus } from '../../../api/processOrder'
import { queries } from '../../../queries'

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
      await queryClient.invalidateQueries({
        queryKey: queries.processOrderDetail.detail(variables.orderUuid).queryKey,
      })
    },
  })
}
