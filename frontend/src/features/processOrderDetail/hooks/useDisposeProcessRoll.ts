import { useMutation, useQueryClient } from '@tanstack/react-query'
import { disposeProcessRoll } from '../../../api/processOrder'
import type { ProcessRollDispositionDTO } from '../../../types/processOrder'
import { invalidateProcessOrderReadModels } from './invalidateProcessOrderReadModels'

interface DisposeProcessRollParams {
  rollUuid: string
  orderUuid: string
  dto: ProcessRollDispositionDTO
}

export function useDisposeProcessRoll() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ rollUuid, dto }: DisposeProcessRollParams) => disposeProcessRoll(rollUuid, dto),
    onSuccess: async (_, variables) => {
      await invalidateProcessOrderReadModels(queryClient, variables.orderUuid)
    },
  })
}
