import { useMutation, useQueryClient } from '@tanstack/react-query'
import { exportTaskKeys } from '../queries/exportTaskKeys'
import { exportTaskService } from '../services/exportTaskService'

export function useCreateProcessOrderExportTask() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: exportTaskService.createProcessOrder,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: exportTaskKeys._def }),
  })
}
