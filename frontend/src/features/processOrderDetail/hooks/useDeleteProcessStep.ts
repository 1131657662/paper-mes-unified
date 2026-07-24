import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deleteProcessStep } from '../../../api/processOrder'
import { invalidateProcessOrderReadModels } from './invalidateProcessOrderReadModels'

interface DeleteStepParams {
  orderUuid: string
  stepUuid: string
}

export function useDeleteProcessStep() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ stepUuid }: DeleteStepParams) => deleteProcessStep(stepUuid),
    onSuccess: async (_, variables) => {
      await invalidateProcessOrderReadModels(queryClient, variables.orderUuid)
    },
  })
}
