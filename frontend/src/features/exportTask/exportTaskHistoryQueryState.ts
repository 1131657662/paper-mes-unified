import type { ExportTaskHistoryQuery } from '../../types/exportTask'

export type ExportTaskHistoryQueryAction =
  | { type: 'attention'; attentionOnly: boolean }
  | { type: 'keyword'; keyword?: string }
  | { type: 'module'; moduleCode?: string }
  | { type: 'operation'; operationCode?: string }
  | { type: 'page'; current: number; size: number }
  | { type: 'status'; taskStatus?: number }

export const initialExportTaskHistoryQuery: ExportTaskHistoryQuery = { current: 1, size: 10 }

const attentionStatuses = [3, 4, 6]

export function exportTaskHistoryQueryReducer(
  query: ExportTaskHistoryQuery,
  action: ExportTaskHistoryQueryAction,
): ExportTaskHistoryQuery {
  switch (action.type) {
    case 'attention':
      return { ...query, current: 1, attentionOnly: action.attentionOnly,
        taskStatus: action.attentionOnly && query.taskStatus !== undefined
          && !attentionStatuses.includes(query.taskStatus) ? undefined : query.taskStatus }
    case 'keyword':
      return { ...query, current: 1, keyword: action.keyword?.trim() || undefined }
    case 'module':
      return { ...query, current: 1, moduleCode: action.moduleCode }
    case 'operation':
      return { ...query, current: 1, operationCode: action.operationCode }
    case 'page':
      return { ...query, current: action.size === query.size ? action.current : 1, size: action.size }
    case 'status':
      return { ...query, current: 1, taskStatus: action.taskStatus,
        attentionOnly: action.taskStatus !== undefined && !attentionStatuses.includes(action.taskStatus)
          ? false : query.attentionOnly }
    default: {
      const exhaustive: never = action
      return exhaustive
    }
  }
}
