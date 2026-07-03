import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateProcessOrderRemark } from '../../../api/processOrder'
import type { ProcessOrderRemarkDTO } from '../../../types/processOrder'
import { queries } from '../../../queries'

interface UpdateOrderRemarkParams {
  orderUuid: string
  values: ProcessOrderRemarkDTO
}

export function useUpdateOrderRemark() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ orderUuid, values }: UpdateOrderRemarkParams) => updateProcessOrderRemark(orderUuid, values),
    onSuccess: async (_, variables) => {
      await queryClient.invalidateQueries({
        queryKey: queries.processOrderDetail.detail(variables.orderUuid).queryKey,
      })
    },
  })
}
