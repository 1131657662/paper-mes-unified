import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateOriginalRollRemark } from '../../../api/processOrder'
import type { OriginalRollRemarkDTO } from '../../../types/processOrder'
import { queries } from '../../../queries'

interface UpdateRollRemarkParams {
  orderUuid: string
  rollUuid: string
  values: OriginalRollRemarkDTO
}

export function useUpdateRollRemark() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ rollUuid, values }: UpdateRollRemarkParams) => updateOriginalRollRemark(rollUuid, values),
    onSuccess: async (_, variables) => {
      await queryClient.invalidateQueries({
        queryKey: queries.processOrderDetail.detail(variables.orderUuid).queryKey,
      })
    },
  })
}
