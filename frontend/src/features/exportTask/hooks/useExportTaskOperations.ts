import { useQuery } from '@tanstack/react-query'
import { exportTaskKeys } from '../queries/exportTaskKeys'

export function useExportTaskOperations(enabled: boolean) {
  return useQuery({
    ...exportTaskKeys.operations,
    enabled,
    refetchInterval: enabled ? 15_000 : false,
  })
}
