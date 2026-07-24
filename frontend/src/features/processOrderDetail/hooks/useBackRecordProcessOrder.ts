import { useMutation, useQueryClient } from '@tanstack/react-query'
import { backRecordProcessOrder } from '../../../api/processOrder'
import type { BackRecordDTO } from '../../../types/processOrder'
import { invalidateProcessOrderReadModels } from './invalidateProcessOrderReadModels'

export function useBackRecordProcessOrder(orderUuid?: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (dto: BackRecordDTO) => {
      if (!orderUuid) throw new Error('缺少加工单ID')
      return backRecordProcessOrder(orderUuid, dto)
    },
    onSuccess: async () => {
      if (!orderUuid) return
      await invalidateProcessOrderReadModels(queryClient, orderUuid)
    },
  })
}
