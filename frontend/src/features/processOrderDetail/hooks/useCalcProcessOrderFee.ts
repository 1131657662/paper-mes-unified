import { useMutation, useQueryClient } from '@tanstack/react-query'
import { calcProcessOrderFee } from '../../../api/processOrder'
import { queries } from '../../../queries'

export function useCalcProcessOrderFee(orderUuid?: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => calcProcessOrderFee(orderUuid!),
    onSuccess: async () => {
      if (!orderUuid) return
      await queryClient.invalidateQueries({
        queryKey: queries.processOrderDetail.detail(orderUuid).queryKey,
      })
    },
  })
}
