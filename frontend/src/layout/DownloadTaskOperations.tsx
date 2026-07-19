import { ReloadOutlined } from '@ant-design/icons'
import { Alert, Badge, Button, Progress, Skeleton, Space, Tooltip, Typography } from 'antd'
import dayjs from 'dayjs'
import {
  exportOperationsNeedAttention,
  exportExecutorQueueUsagePercent,
  formatOperationDuration,
  formatStoredFileBytes,
  exportStorageStatusLabel,
} from '../features/exportTask/exportTaskOperationsDisplay'
import { useExportTaskOperations } from '../features/exportTask/hooks/useExportTaskOperations'
import { useRefreshExportTaskOperations } from '../features/exportTask/hooks/useRefreshExportTaskOperations'
import type { ExportTaskOperations } from '../types/exportTask'
import DownloadTaskOperationIssues from './DownloadTaskOperationIssues'

interface Props {
  enabled: boolean
}

export default function DownloadTaskOperations({ enabled }: Props) {
  const refreshOperations = useRefreshExportTaskOperations()
  const {
    data: operations,
    isError: isOperationsError,
    isFetching: isFetchingOperations,
    isLoading: isLoadingOperations,
  } = useExportTaskOperations(enabled)

  if (isLoadingOperations) return <Skeleton active paragraph={{ rows: 4 }} />
  if (isOperationsError) {
    return <Alert type="error" showIcon message="运行状态加载失败" action={(
      <Button size="small" onClick={() => void refreshOperations()}>重试</Button>
    )} />
  }
  if (!operations) return null
  return <div className="download-task-operations-stack">
    <OperationsOverview operations={operations} refreshing={isFetchingOperations}
      onRefresh={() => void refreshOperations()} />
    <DownloadTaskOperationIssues enabled={enabled} />
  </div>
}

interface OverviewProps {
  onRefresh: () => void
  operations: ExportTaskOperations
  refreshing?: boolean
}

export function OperationsOverview({ operations, onRefresh, refreshing }: OverviewProps) {
  const needsAttention = exportOperationsNeedAttention(operations)
  return (
    <section className="download-task-operations" aria-label="导出任务运行状态">
      <div className="download-task-operations__heading">
        <Badge status={needsAttention ? 'warning' : 'success'} text={needsAttention ? '需要关注' : '运行正常'} />
        <Space size={4}>
          <Typography.Text type="secondary">更新于 {dayjs(operations.asOf).format('HH:mm:ss')}</Typography.Text>
          <Tooltip title="同时刷新指标和异常明细">
            <Button type="text" size="small" icon={<ReloadOutlined />} loading={refreshing}
              aria-label="刷新运行状态和异常明细" onClick={onRefresh} />
          </Tooltip>
        </Space>
      </div>
      <OperationsMetricStrip operations={operations} />
      <ExecutorCapacity operations={operations} />
      <StorageHealth operations={operations} />
      <OperationsDetails operations={operations} />
      {operations.oldestQueuedAt && (
        <Typography.Text type="secondary" className="download-task-operations__oldest">
          最早排队于 {dayjs(operations.oldestQueuedAt).format('MM-DD HH:mm:ss')}
        </Typography.Text>
      )}
    </section>
  )
}

function StorageHealth({ operations }: { operations: ExportTaskOperations }) {
  const ready = operations.storageAvailable && operations.storageWritable
  const statusLabel = exportStorageStatusLabel(operations.storageStatus)
  const message = ready ? '新导出任务可正常写入' : '新导出任务已暂停，请检查共享存储'
  return <div className={`download-task-operations__storage${ready ? '' : ' is-warning'}`}>
    <div className="download-task-operations__storage-heading">
      <span>文件存储</span><strong>{statusLabel}</strong>
    </div>
    <div className="download-task-operations__storage-values">
      <span>可用 {formatStoredFileBytes(operations.storageFreeBytes)}</span>
      <span>总容量 {formatStoredFileBytes(operations.storageTotalBytes)}</span>
      <span>剩余 {operations.storageFreePercent.toFixed(1)}%</span>
    </div>
    <Typography.Text type={ready ? 'secondary' : 'danger'}>{message}</Typography.Text>
  </div>
}

function ExecutorCapacity({ operations }: { operations: ExportTaskOperations }) {
  const queueUsage = exportExecutorQueueUsagePercent(operations)
  return <div className="download-task-operations__capacity">
    <div className="download-task-operations__capacity-row">
      <span>执行线程</span>
      <strong>{operations.activeWorkerCount} / {operations.workerCount}</strong>
    </div>
    <div className="download-task-operations__capacity-row">
      <span>内存队列</span>
      <strong>{operations.queuedInMemoryCount} / {operations.queueCapacity}</strong>
    </div>
    <Progress percent={queueUsage} showInfo={false} size="small"
      status={queueUsage >= 80 ? 'exception' : 'normal'} />
    <Typography.Text type={operations.rejectedSubmissionCount > 0 ? 'warning' : 'secondary'}>
      本次启动：线程完成 {operations.completedExecutionCount.toLocaleString()} · 容量拒绝{' '}
      {operations.rejectedSubmissionCount.toLocaleString()}
    </Typography.Text>
  </div>
}

function OperationsMetricStrip({ operations }: { operations: ExportTaskOperations }) {
  const metrics = [
    { label: '排队', value: operations.queuedCount },
    { label: '运行中', value: operations.runningCount },
    { label: '执行停滞', value: operations.staleRunningCount, warning: operations.staleRunningCount > 0 },
    { label: '24 小时失败', value: operations.failedLast24Hours, warning: operations.failedLast24Hours > 0 },
  ]
  return <div className="download-task-operations__metrics">
    {metrics.map((metric) => (
      <div className={`download-task-operations__metric${metric.warning ? ' is-warning' : ''}`} key={metric.label}>
        <span>{metric.label}</span><strong>{metric.value}</strong>
      </div>
    ))}
  </div>
}

function OperationsDetails({ operations }: { operations: ExportTaskOperations }) {
  const details = [
    { label: '24 小时成功', value: operations.succeededLast24Hours.toLocaleString() },
    { label: '平均耗时', value: formatOperationDuration(operations.averageDurationSeconds) },
    { label: '文件占用', value: formatStoredFileBytes(operations.storedFileBytes) },
    { label: '实时连接', value: operations.sseConnectionCount.toLocaleString() },
  ]
  return <dl className="download-task-operations__details">
    {details.map((detail) => <div key={detail.label}><dt>{detail.label}</dt><dd>{detail.value}</dd></div>)}
  </dl>
}
