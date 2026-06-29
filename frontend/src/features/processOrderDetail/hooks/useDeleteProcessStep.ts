import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deleteProcessStep } from '../../../api/processOrder'
import { queries } from '../../../queries'

interface DeleteStepParams {
  orderUuid: string
  stepUuid: string
}

export function useDeleteProcessStep() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ stepUuid }: DeleteStepParams) => deleteProcessStep(stepUuid),
    onSuccess: async (_, variables) => {
      await queryClient.invalidateQueries({
        queryKey: queries.processOrderDetail.detail(variables.orderUuid).queryKey,
      })
    },
  })
}
