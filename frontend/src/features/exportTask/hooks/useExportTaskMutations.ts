import { useMutation, useQueryClient } from '@tanstack/react-query'
import { exportTaskKeys } from '../queries/exportTaskKeys'
import { exportTaskService } from '../services/exportTaskService'

export function useCreateSettleExportTask() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: exportTaskService.createSettle,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: exportTaskKeys._def }),
  })
}

export function useAcknowledgeExportTasks() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: exportTaskService.acknowledge,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: exportTaskKeys._def }),
  })
}

export function useDownloadExportTask() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: exportTaskService.download,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: exportTaskKeys._def }),
  })
}

export function useRetryExportTask() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: exportTaskService.retry,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: exportTaskKeys._def }),
  })
}

export function useCancelExportTask() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: exportTaskService.cancel,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: exportTaskKeys._def }),
  })
}

export function useAcknowledgeExportTask() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: exportTaskService.acknowledgeOne,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: exportTaskKeys._def }),
  })
}
