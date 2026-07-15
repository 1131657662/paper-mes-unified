import { useMutation, useQueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { DataHealthRepairRequest } from '../../../types/dataHealth'
import { dataHealthService } from '../services/dataHealthService'

export function useRestoreCompletedOrder() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ uuid, data }: { uuid: string; data: DataHealthRepairRequest }) =>
      dataHealthService.restoreCompletedOrder(uuid, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queries.dataHealth._def }),
  })
}
