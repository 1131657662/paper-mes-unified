import { keepPreviousData, useQuery } from '@tanstack/react-query'
import type { ExportTaskHistoryQuery } from '../../../types/exportTask'
import { exportTaskKeys } from '../queries/exportTaskKeys'

export function useExportTaskHistory(query: ExportTaskHistoryQuery, enabled: boolean) {
  return useQuery({
    ...exportTaskKeys.history(query),
    enabled,
    placeholderData: keepPreviousData,
  })
}
