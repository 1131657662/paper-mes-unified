import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateProcessStep, type ProcessStepDTO } from '../../../api/processOrder'
import { invalidateProcessOrderReadModels } from './invalidateProcessOrderReadModels'

interface UpdateStepParams {
  orderUuid: string
  stepUuid: string
  values: ProcessStepDTO
}

export function useUpdateProcessStep() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ stepUuid, values }: UpdateStepParams) => updateProcessStep(stepUuid, values),
    onSuccess: async (_, variables) => {
      await invalidateProcessOrderReadModels(queryClient, variables.orderUuid)
    },
  })
}
