import { Alert, Button, Skeleton, Tag, Typography } from 'antd'
import dayjs from 'dayjs'
import { exportTaskModuleLabel } from '../features/exportTask/exportTaskDisplay'
import { useExportTaskOperationsIssues } from '../features/exportTask/hooks/useExportTaskOperationsIssues'
import type { ExportTaskOperationsIssue, ExportTaskOperationsIssues } from '../types/exportTask'

interface Props {
  enabled: boolean
}

export default function DownloadTaskOperationIssues({ enabled }: Props) {
  const { data: issues, isError, isLoading, refetch: refetchIssues } = useExportTaskOperationsIssues(enabled)

  if (isLoading) return <Skeleton active paragraph={{ rows: 3 }} />
  if (isError) return <Alert type="warning" showIcon message="异常任务明细加载失败" action={(
    <Button size="small" onClick={() => void refetchIssues()}>重试</Button>
  )} />
  if (!issues) return null
  return <OperationsIssuesView issues={issues} />
}

export function OperationsIssuesView({ issues }: { issues: ExportTaskOperationsIssues }) {
  if (issues.staleTasks.length === 0 && issues.failedTasks.length === 0) {
    return <Typography.Text type="secondary" className="download-task-operation-issues__empty">
      暂无停滞或近 24 小时失败任务
    </Typography.Text>
  }
  return <section className="download-task-operation-issues" aria-label="异常任务明细">
    <Typography.Title level={5}>异常任务</Typography.Title>
    <IssueGroup title="执行停滞" kind="stale" items={issues.staleTasks} />
    <IssueGroup title="24 小时失败" kind="failed" items={issues.failedTasks} />
  </section>
}

interface GroupProps {
  items: ExportTaskOperationsIssue[]
  kind: 'failed' | 'stale'
  title: string
}

function IssueGroup({ items, kind, title }: GroupProps) {
  if (items.length === 0) return null
  return <div className="download-task-operation-issues__group">
    <div className="download-task-operation-issues__group-title"><span>{title}</span><strong>{items.length}</strong></div>
    {items.map((item) => <IssueRow key={item.uuid} item={item} kind={kind} />)}
  </div>
}

function IssueRow({ item, kind }: { item: ExportTaskOperationsIssue; kind: GroupProps['kind'] }) {
  const timestamp = kind === 'stale' ? item.heartbeatAt || item.createTime : item.completedAt || item.createTime
  return <div className="download-task-operation-issues__item">
    <div className="download-task-operation-issues__item-title">
      <Typography.Text strong ellipsis>{item.taskName}</Typography.Text>
      <Tag color={kind === 'stale' ? 'warning' : 'error'}>{kind === 'stale' ? '停滞' : '失败'}</Tag>
    </div>
    <Typography.Text type="secondary">
      {item.requesterName} · {exportTaskModuleLabel(item.moduleCode)} · {dayjs(timestamp).format('MM-DD HH:mm:ss')}
    </Typography.Text>
    {kind === 'failed' && item.errorMessage && <Typography.Text type="danger">{item.errorMessage}</Typography.Text>}
  </div>
}
