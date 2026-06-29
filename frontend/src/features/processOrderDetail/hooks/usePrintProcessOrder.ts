import { useMutation, useQueryClient } from '@tanstack/react-query'
import { printProcessOrder } from '../../../api/processOrder'
import type { PrintDTO } from '../../../types/processOrder'
import { queries } from '../../../queries'

export function usePrintProcessOrder(orderUuid?: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (dto?: PrintDTO) => printProcessOrder(orderUuid!, dto),
    onSuccess: async () => {
      if (!orderUuid) return
      await queryClient.invalidateQueries({
        queryKey: queries.processOrderDetail.detail(orderUuid).queryKey,
      })
    },
  })
}
