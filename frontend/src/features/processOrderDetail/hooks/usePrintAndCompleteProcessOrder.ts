import { useMutation, useQueryClient } from '@tanstack/react-query'
import { printAndCompleteProcessOrder } from '../../../api/processOrder'
import type { PrintDTO } from '../../../types/processOrder'
import { invalidateProcessOrderReadModels } from './invalidateProcessOrderReadModels'

export function usePrintAndCompleteProcessOrder(orderUuid?: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (dto?: PrintDTO) => printAndCompleteProcessOrder(orderUuid!, dto),
    onSuccess: async () => {
      if (!orderUuid) return
      await invalidateProcessOrderReadModels(queryClient, orderUuid)
    },
  })
}
