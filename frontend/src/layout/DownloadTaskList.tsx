import { CheckOutlined, CloseOutlined, DownloadOutlined, FileExcelOutlined, ReloadOutlined } from '@ant-design/icons'
import { Button, Empty, List, Popconfirm, Progress, Space, Tag, Tooltip, Typography } from 'antd'
import dayjs from 'dayjs'
import { Link } from 'react-router-dom'
import {
  exportTaskActions,
  exportTaskModuleLabel,
  exportTaskOperationLabel,
  exportTaskRetryLimitReached,
  exportTaskStatus,
  type ExportTaskAction,
} from '../features/exportTask/exportTaskDisplay'
import type { ExportTask } from '../types/exportTask'

interface Props {
  items: ExportTask[]
  handlers: TaskHandlers
}

export interface TaskHandlers {
  onDownload: (task: ExportTask) => void
  onRetry: (task: ExportTask) => void
  onCancel: (task: ExportTask) => void
  onAcknowledge: (task: ExportTask) => void
  onOpenSource: () => void
  busy: (taskUuid: string) => { uuid: string; action: ExportTaskAction } | undefined
}

const sourceRoutes: Partial<Record<string, (uuid: string) => string>> = {
  SETTLE_DETAIL: (uuid) => `/settle-orders/${encodeURIComponent(uuid)}`,
  PROCESS_ORDER_DETAIL: (uuid) => `/process-orders/${encodeURIComponent(uuid)}`,
  DELIVERY_ORDER_DETAIL: (uuid) => `/delivery-orders/${encodeURIComponent(uuid)}`,
}

export default function DownloadTaskList({ items, handlers }: Props) {
  if (items.length === 0) return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无导出任务" />
  return <List className="download-task-center__list" dataSource={items}
    renderItem={(task) => <TaskListItem task={task} handlers={handlers} />} />
}

function TaskListItem({ task, handlers }: { task: ExportTask; handlers: TaskHandlers }) {
  const status = exportTaskStatus[task.taskStatus] ?? { color: 'error', text: '未知状态' }
  const actions = exportTaskActions(task)
  const itemActions = actions.length > 0
    ? [<TaskActions key="actions" task={task} actions={actions} handlers={handlers} />]
    : undefined
  const sourcePath = resolveSourcePath(task)
  return (
    <List.Item actions={itemActions}>
      <div className="download-task-center__item">
        <div className="download-task-center__title">
          <TaskTitle task={task} sourcePath={sourcePath} onOpenSource={handlers.onOpenSource} />
          <Space size={4} wrap>
            <Tag color="blue">{exportTaskModuleLabel(task.moduleCode)}</Tag>
            <Tag>{exportTaskOperationLabel(task.operationCode)}</Tag>
            <Tag color={status.color}>{status.text}</Tag>
            {!task.resourceAccessible && <Tag>权限已变更</Tag>}
          </Space>
        </div>
        <TaskFileDetails task={task} />
        <TaskAuditContext task={task} />
        {[1, 2].includes(task.taskStatus) && <Progress percent={task.progress} size="small" />}
        {task.taskStatus === 4 && <Typography.Text type="danger">{task.errorMessage || '导出失败，请重新发起'}</Typography.Text>}
        <RetryBudgetNotice task={task} />
        <TaskMetadata task={task} />
      </div>
    </List.Item>
  )
}

function resolveSourcePath(task: ExportTask) {
  if (!task.resourceAccessible) return undefined
  if (task.moduleCode === 'report' && task.sourcePath?.startsWith('/reports/')) return task.sourcePath
  return sourceRoutes[task.taskType]?.(task.sourceUuid)
}

function TaskAuditContext({ task }: { task: ExportTask }) {
  if (!task.querySnapshotUuid && !task.metricReleaseUuid) return null
  return <div className="download-task-center__audit">
    {task.querySnapshotUuid && <Tooltip title={`查询快照 ${task.querySnapshotUuid}`}>
      <Typography.Text type="secondary">快照 {shortId(task.querySnapshotUuid)}</Typography.Text>
    </Tooltip>}
    {task.metricReleaseUuid && <Tooltip title={`指标发布包 ${task.metricReleaseUuid}`}>
      <Typography.Text type="secondary">口径 {shortId(task.metricReleaseUuid)}</Typography.Text>
    </Tooltip>}
  </div>
}

function TaskTitle({ task, sourcePath, onOpenSource }: {
  task: ExportTask
  sourcePath?: string
  onOpenSource: () => void
}) {
  if (!sourcePath) return <Typography.Text strong ellipsis>{task.taskName}</Typography.Text>
  return <Tooltip title={`查看来源：${task.taskName}`} placement="topLeft">
    <Link className="download-task-center__source-link" to={sourcePath} onClick={onOpenSource}
      aria-label={`查看来源：${task.taskName}`}>
      {task.taskName}
    </Link>
  </Tooltip>
}

function TaskFileDetails({ task }: { task: ExportTask }) {
  if (!task.fileName || ![3, 6].includes(task.taskStatus)) return null
  return (
    <div className="download-task-center__file">
      <FileExcelOutlined aria-hidden />
      <Tooltip title={task.fileName} placement="topLeft">
        <Typography.Text ellipsis>{task.fileName}</Typography.Text>
      </Tooltip>
    </div>
  )
}

function TaskMetadata({ task }: { task: ExportTask }) {
  const hasFile = Boolean(task.fileName) && [3, 6].includes(task.taskStatus)
  return (
    <div className="download-task-center__metadata">
      <Typography.Text type="secondary">
        {task.fileSize ? formatSize(task.fileSize) : 'Excel'} · {dayjs(task.createTime).format('MM-DD HH:mm:ss')}
      </Typography.Text>
      {hasFile && <FileLifecycle task={task} />}
    </div>
  )
}

function FileLifecycle({ task }: { task: ExportTask }) {
  const downloadCount = task.downloadedAt ? Math.max(task.downloadCount ?? 0, 1) : task.downloadCount ?? 0
  return (
    <span className="download-task-center__lifecycle">
      <Typography.Text type="secondary">{downloadCount > 0 ? `已下载 ${downloadCount} 次` : '尚未下载'}</Typography.Text>
      {task.downloadedAt && <Typography.Text type="secondary">
        最近下载 {dayjs(task.downloadedAt).format('MM-DD HH:mm')}
      </Typography.Text>}
      {task.expiresAt && <Typography.Text type="secondary">有效至 {dayjs(task.expiresAt).format('MM-DD HH:mm')}</Typography.Text>}
    </span>
  )
}

function RetryBudgetNotice({ task }: { task: ExportTask }) {
  const hasBudget = task.attemptCount !== undefined && task.maxAttempts !== undefined
  const exhausted = exportTaskRetryLimitReached(task)
  if (!hasBudget || (task.taskStatus !== 4 && !exhausted)) return null
  const text = exhausted
    ? `已达重试上限（${task.attemptCount}/${task.maxAttempts} 次）`
    : `已尝试 ${task.attemptCount}/${task.maxAttempts} 次`
  return <Typography.Text className={exhausted ? 'download-task-center__retry-limit' : undefined}
    type="secondary">{text}</Typography.Text>
}

function TaskActions({ task, actions, handlers }: { task: ExportTask; actions: ExportTaskAction[]; handlers: TaskHandlers }) {
  const busy = handlers.busy(task.uuid)
  const retryLabel = task.taskStatus === 6 ? '重新生成' : '重试'
  return <Space size={0}>
    {actions.includes('download') && <Button type="link" size="small" icon={<DownloadOutlined />}
      loading={busy?.action === 'download'} onClick={() => handlers.onDownload(task)}>下载</Button>}
    {actions.includes('retry') && <Button type="link" size="small" icon={<ReloadOutlined />}
      loading={busy?.action === 'retry'} onClick={() => handlers.onRetry(task)}>{retryLabel}</Button>}
    {actions.includes('cancel') && <Popconfirm
      title="确认取消导出任务？"
      description="仅会取消尚未开始执行的任务，已生成的文件不受影响。"
      okText="确认取消"
      cancelText="继续等待"
      onConfirm={() => handlers.onCancel(task)}
    >
      <Button type="link" size="small" icon={<CloseOutlined />}
        loading={busy?.action === 'cancel'}>取消</Button>
    </Popconfirm>}
    {actions.includes('acknowledge') && <Button type="link" size="small" icon={<CheckOutlined />}
      loading={busy?.action === 'acknowledge'} onClick={() => handlers.onAcknowledge(task)}>已处理</Button>}
  </Space>
}

function formatSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function shortId(value: string) { return value.length <= 8 ? value : value.slice(0, 8) }
