import { useMutation, useQueryClient } from '@tanstack/react-query'
import { addProcessStepsBatch, type ProcessStepBatchDTO } from '../../../api/processOrder'
import { invalidateProcessOrderReadModels } from './invalidateProcessOrderReadModels'

interface AddStepsBatchParams {
  orderUuid: string
  values: ProcessStepBatchDTO
}

export function useAddProcessStepsBatch() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ orderUuid, values }: AddStepsBatchParams) => addProcessStepsBatch(orderUuid, values),
    onSuccess: async (_, variables) => {
      await invalidateProcessOrderReadModels(queryClient, variables.orderUuid)
    },
  })
}
