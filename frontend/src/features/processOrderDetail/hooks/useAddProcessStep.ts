import { useMutation, useQueryClient } from '@tanstack/react-query'
import { addProcessStep, type ProcessStepDTO } from '../../../api/processOrder'
import { invalidateProcessOrderReadModels } from './invalidateProcessOrderReadModels'

interface AddStepParams {
  orderUuid: string
  values: ProcessStepDTO
}

export function useAddProcessStep() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ orderUuid, values }: AddStepParams) => addProcessStep(orderUuid, values),
    onSuccess: async (_, variables) => {
      await invalidateProcessOrderReadModels(queryClient, variables.orderUuid)
    },
  })
}
