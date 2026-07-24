import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateProcessOrderRemark } from '../../../api/processOrder'
import type { ProcessOrderRemarkDTO } from '../../../types/processOrder'
import { invalidateProcessOrderLocalReadModels } from './invalidateProcessOrderReadModels'

interface UpdateOrderRemarkParams {
  orderUuid: string
  values: ProcessOrderRemarkDTO
}

export function useUpdateOrderRemark() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ orderUuid, values }: UpdateOrderRemarkParams) => updateProcessOrderRemark(orderUuid, values),
    onSuccess: async (_, variables) => {
      await invalidateProcessOrderLocalReadModels(queryClient, variables.orderUuid)
    },
  })
}
