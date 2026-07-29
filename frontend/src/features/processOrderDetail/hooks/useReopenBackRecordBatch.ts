import { useMutation, useQueryClient } from '@tanstack/react-query'
import { reopenBackRecordBatch } from '../../../api/processOrder'
import type { BackRecordReopenDTO } from '../../../types/processOrder'
import { invalidateProcessOrderReadModels } from './invalidateProcessOrderReadModels'

interface ReopenBackRecordBatchParams {
  orderUuid: string
  values: BackRecordReopenDTO
}

export function useReopenBackRecordBatch() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ orderUuid, values }: ReopenBackRecordBatchParams) =>
      reopenBackRecordBatch(orderUuid, values),
    onSuccess: async (_, variables) => {
      await invalidateProcessOrderReadModels(queryClient, variables.orderUuid)
    },
  })
}
