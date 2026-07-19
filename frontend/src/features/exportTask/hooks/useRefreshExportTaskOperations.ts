import { useQueryClient } from '@tanstack/react-query'
import { exportTaskKeys } from '../queries/exportTaskKeys'

export function useRefreshExportTaskOperations() {
  const queryClient = useQueryClient()
  return () => Promise.all([
    queryClient.refetchQueries({ queryKey: exportTaskKeys.operations.queryKey, exact: true }),
    queryClient.refetchQueries({ queryKey: exportTaskKeys.operationsIssues.queryKey, exact: true }),
  ])
}
