import { createQueryKeys } from '@lukemorales/query-key-factory'
import type { ExportTaskHistoryQuery } from '../../../types/exportTask'
import { exportTaskService } from '../services/exportTaskService'

export const exportTaskKeys = createQueryKeys('exportTasks', {
  summary: {
    queryKey: null,
    queryFn: exportTaskService.summary,
  },
  operations: {
    queryKey: null,
    queryFn: exportTaskService.operations,
  },
  operationsIssues: {
    queryKey: null,
    queryFn: exportTaskService.operationsIssues,
  },
  history: (query: ExportTaskHistoryQuery) => ({
    queryKey: [query],
    queryFn: () => exportTaskService.history(query),
  }),
})
