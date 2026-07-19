import { useQuery } from '@tanstack/react-query'
import { exportTaskKeys } from '../queries/exportTaskKeys'

export function useExportTaskOperationsIssues(enabled: boolean) {
  return useQuery({
    ...exportTaskKeys.operationsIssues,
    enabled,
    refetchInterval: enabled ? 15_000 : false,
  })
}
