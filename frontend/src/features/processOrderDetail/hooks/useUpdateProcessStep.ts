import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateProcessStep, type ProcessStepDTO } from '../../../api/processOrder'
import { queries } from '../../../queries'

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
      await queryClient.invalidateQueries({
        queryKey: queries.processOrderDetail.detail(variables.orderUuid).queryKey,
      })
    },
  })
}
