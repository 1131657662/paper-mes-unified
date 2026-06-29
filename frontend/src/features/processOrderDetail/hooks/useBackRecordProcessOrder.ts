import { useMutation, useQueryClient } from '@tanstack/react-query'
import { backRecordProcessOrder } from '../../../api/processOrder'
import { queries } from '../../../queries'
import type { BackRecordDTO } from '../../../types/processOrder'

export function useBackRecordProcessOrder(orderUuid?: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (dto: BackRecordDTO) => {
      if (!orderUuid) throw new Error('缺少加工单ID')
      return backRecordProcessOrder(orderUuid, dto)
    },
    onSuccess: () => {
      if (!orderUuid) return
      queryClient.invalidateQueries({
        queryKey: queries.processOrderDetail.detail(orderUuid).queryKey,
      })
    },
  })
}
