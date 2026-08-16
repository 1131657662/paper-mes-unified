import { useMutation, useQueryClient } from '@tanstack/react-query'
import { completeBackRecordOrder } from '../../../api/processOrder'
import type { BackRecordCompleteDTO } from '../../../types/processOrder'
import { invalidateProcessOrderReadModels } from './invalidateProcessOrderReadModels'

export function useCompleteBackRecordOrder(orderUuid?: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (dto: BackRecordCompleteDTO) => {
      if (!orderUuid) throw new Error('缺少加工单ID')
      return completeBackRecordOrder(orderUuid, dto)
    },
    onSuccess: async () => {
      if (!orderUuid) return
      await invalidateProcessOrderReadModels(queryClient, orderUuid)
    },
  })
}
