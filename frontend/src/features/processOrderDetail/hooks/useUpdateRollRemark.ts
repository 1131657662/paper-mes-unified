import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateOriginalRollRemark } from '../../../api/processOrder'
import type { OriginalRollRemarkDTO } from '../../../types/processOrder'
import { invalidateProcessOrderLocalReadModels } from './invalidateProcessOrderReadModels'

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
      await invalidateProcessOrderLocalReadModels(queryClient, variables.orderUuid)
    },
  })
}
