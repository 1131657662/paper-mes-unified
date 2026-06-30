import { useMutation, useQueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'
import { settleService } from '../services/settleService'

export function useVoidSettle() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: settleService.void,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queries.settle._def })
    },
  })
}
