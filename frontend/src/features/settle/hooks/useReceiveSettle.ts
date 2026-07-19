import { useMutation, useQueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'
import { settleService } from '../services/settleService'

export function useReceiveSettle() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: settleService.receive,
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: queries.settle.detail(variables.uuid).queryKey })
      queryClient.invalidateQueries({ queryKey: queries.settle.detailHeader(variables.uuid).queryKey })
      queryClient.invalidateQueries({ queryKey: queries.settle.receives(variables.uuid).queryKey })
      queryClient.invalidateQueries({ queryKey: queries.settle.list._def })
      queryClient.invalidateQueries({ queryKey: queries.settle.summary._def })
    },
  })
}
