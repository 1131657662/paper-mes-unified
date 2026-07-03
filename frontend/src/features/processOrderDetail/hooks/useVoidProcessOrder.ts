import { useMutation, useQueryClient } from '@tanstack/react-query'
import { voidProcessOrder } from '../../../api/processOrder'
import { queries } from '../../../queries'

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
      await queryClient.invalidateQueries({
        queryKey: queries.processOrderDetail.detail(variables.orderUuid).queryKey,
      })
    },
  })
}
