import { useMutation, useQueryClient } from '@tanstack/react-query'
import { exportTaskKeys } from '../queries/exportTaskKeys'
import { exportTaskService } from '../services/exportTaskService'

export function useCreateDeliveryOrderExportTask() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: exportTaskService.createDeliveryOrder,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: exportTaskKeys._def }),
  })
}
