import { useQuery } from '@tanstack/react-query'
import { exportTaskKeys } from '../queries/exportTaskKeys'

export function useExportTasks(enabled: boolean) {
  return useQuery({
    ...exportTaskKeys.summary,
    enabled,
    refetchInterval: (query) => {
      if (!enabled) return false
      return (query.state.data?.runningCount ?? 0) > 0 ? 5_000 : 30_000
    },
  })
}
