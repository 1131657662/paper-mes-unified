import type { PageResult } from './common'

export interface ExportTask {
  uuid: string
  taskType: string
  moduleCode?: string
  operationCode?: string
  taskName: string
  sourceUuid: string
  taskStatus: number
  progress: number
  fileName?: string
  fileSize?: number
  errorMessage?: string
  createTime: string
  completedAt?: string
  expiresAt?: string
  downloadedAt?: string
  downloadCount?: number
  attemptCount?: number
  maxAttempts?: number
  acknowledged: boolean
  resourceAccessible: boolean
}

export interface ExportTaskSummary {
  runningCount: number
  unacknowledgedCount: number
}

export interface ExportTaskHistoryQuery {
  current: number
  size: number
  taskStatus?: number
  moduleCode?: string
  keyword?: string
  attentionOnly?: boolean
}

export interface ExportTaskHistoryPage extends PageResult<ExportTask> {
  asOf: string
}

export type ExportTaskAcknowledgeFilter = Pick<
  ExportTaskHistoryQuery,
  'taskStatus' | 'moduleCode' | 'keyword'
> & { asOf: string }

export interface ExportTaskOperations {
  queuedCount: number
  runningCount: number
  succeededLast24Hours: number
  failedLast24Hours: number
  staleRunningCount: number
  oldestQueuedAt?: string
  averageDurationSeconds: number
  storedFileBytes: number
  sseConnectionCount: number
  workerCount: number
  activeWorkerCount: number
  queuedInMemoryCount: number
  queueCapacity: number
  rejectedSubmissionCount: number
  completedExecutionCount: number
  storageStatus: string
  storageAvailable: boolean
  storageWritable: boolean
  storageFreeBytes: number
  storageTotalBytes: number
  storageFreePercent: number
  storageCheckedAt: string
  asOf: string
}

export interface ExportTaskOperationsIssue {
  uuid: string
  taskName: string
  requesterName: string
  moduleCode: string
  taskStatus: number
  errorMessage?: string
  createTime: string
  heartbeatAt?: string
  completedAt?: string
}

export interface ExportTaskOperationsIssues {
  staleTasks: ExportTaskOperationsIssue[]
  failedTasks: ExportTaskOperationsIssue[]
  asOf: string
}
