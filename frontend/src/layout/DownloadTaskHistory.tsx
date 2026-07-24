import { Alert, Button, Skeleton } from 'antd'
import { useReducer } from 'react'
import {
  exportTaskHistoryQueryReducer,
  initialExportTaskHistoryQuery,
} from '../features/exportTask/exportTaskHistoryQueryState'
import { useExportTaskHistory } from '../features/exportTask/hooks/useExportTaskHistory'
import type { ExportTaskHistoryPage, ExportTaskHistoryQuery } from '../types/exportTask'
import DownloadTaskHistoryToolbar from './DownloadTaskHistoryToolbar'
import DownloadTaskList, { type TaskHandlers } from './DownloadTaskList'
import DownloadTaskPagination from './DownloadTaskPagination'

interface Props {
  enabled: boolean
  handlers: TaskHandlers
  unacknowledgedCount: number
}

export default function DownloadTaskHistory({ enabled, handlers, unacknowledgedCount }: Props) {
  const [query, dispatch] = useReducer(exportTaskHistoryQueryReducer, initialExportTaskHistoryQuery)
  const { data: history, isError, isFetching, isLoading, isPlaceholderData, refetch } =
    useExportTaskHistory(query, enabled)

  if (isLoading) return <Skeleton active paragraph={{ rows: 5 }} />
  if (isError) return <HistoryLoadError onRetry={() => void refetch()} />
  return <section className="download-task-history">
    <DownloadTaskHistoryToolbar query={query} snapshot={history ? {
      asOf: history.asOf, current: !isPlaceholderData, unacknowledgedCount,
    } : undefined}
      onKeywordChange={(keyword) => dispatch({ type: 'keyword', keyword })}
      onStatusChange={(taskStatus) => dispatch({ type: 'status', taskStatus })}
      onModuleChange={(moduleCode) => dispatch({ type: 'module', moduleCode })}
      onOperationChange={(operationCode) => dispatch({ type: 'operation', operationCode })}
      onAttentionChange={(attentionOnly) => dispatch({ type: 'attention', attentionOnly })} />
    <DownloadTaskHistoryResults history={history} handlers={handlers} isFetching={isFetching} query={query}
      onPageChange={(current, size) => dispatch({ type: 'page', current, size })} />
  </section>
}

interface ResultsProps {
  handlers: TaskHandlers
  history?: ExportTaskHistoryPage
  isFetching: boolean
  onPageChange: (current: number, size: number) => void
  query: ExportTaskHistoryQuery
}

export function DownloadTaskHistoryResults(props: ResultsProps) {
  const className = `download-task-history__results${props.isFetching ? ' is-refreshing' : ''}`
  return <div className={className} aria-busy={props.isFetching}>
    <DownloadTaskList items={props.history?.records ?? []} handlers={props.handlers} />
    <DownloadTaskPagination current={props.history?.current ?? props.query.current}
      size={props.history?.size ?? props.query.size} total={props.history?.total ?? 0}
      onChange={props.onPageChange} />
  </div>
}

function HistoryLoadError({ onRetry }: { onRetry: () => void }) {
  return <Alert type="error" showIcon message="任务历史加载失败"
    action={<Button size="small" onClick={onRetry}>重试</Button>} />
}
