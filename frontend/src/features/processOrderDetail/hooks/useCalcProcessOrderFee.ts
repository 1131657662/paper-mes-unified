import { useMutation, useQueryClient } from '@tanstack/react-query'
import { calcProcessOrderFee } from '../../../api/processOrder'
import { invalidateProcessOrderReadModels } from './invalidateProcessOrderReadModels'

export function useCalcProcessOrderFee(orderUuid?: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => calcProcessOrderFee(orderUuid!),
    onSuccess: async () => {
      if (!orderUuid) return
      await invalidateProcessOrderReadModels(queryClient, orderUuid)
    },
  })
}
