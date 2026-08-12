import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateProcessOrderPostProductionNote } from '../../../api/processOrder'
import type { ProcessOrderPostProductionNoteDTO } from '../../../types/processOrder'
import { invalidateProcessOrderLocalReadModels } from './invalidateProcessOrderReadModels'

interface Params { orderUuid: string; values: ProcessOrderPostProductionNoteDTO }

export function useUpdatePostProductionNote() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ orderUuid, values }: Params) => updateProcessOrderPostProductionNote(orderUuid, values),
    onSuccess: async (_, variables) => invalidateProcessOrderLocalReadModels(queryClient, variables.orderUuid),
  })
}
