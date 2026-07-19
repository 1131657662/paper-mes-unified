import type { ExportTaskOperations } from '../../types/exportTask'

export function formatOperationDuration(seconds: number): string {
  if (seconds < 1) return '< 1 秒'
  if (seconds < 60) return `${Math.round(seconds)} 秒`
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = Math.round(seconds % 60)
  return remainingSeconds === 0 ? `${minutes} 分钟` : `${minutes} 分 ${remainingSeconds} 秒`
}

export function formatStoredFileBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`
  return `${(bytes / 1024 / 1024 / 1024).toFixed(1)} GB`
}

export function exportOperationsNeedAttention(operations: ExportTaskOperations): boolean {
  return !operations.storageAvailable
    || !operations.storageWritable
    || operations.staleRunningCount > 0
    || operations.failedLast24Hours > 0
    || operations.rejectedSubmissionCount > 0
    || exportExecutorQueueUsagePercent(operations) >= 80
}

export function exportStorageStatusLabel(status: string): string {
  const labels: Record<string, string> = {
    READY: '共享目录正常',
    LOW_SPACE: '磁盘空间不足',
    READ_ONLY: '目录不可写',
    UNAVAILABLE: '目录不可用',
    ERROR: '检查失败',
  }
  return labels[status] ?? '状态未知'
}

export function exportExecutorQueueUsagePercent(operations: ExportTaskOperations): number {
  if (operations.queueCapacity <= 0) return 0
  return Math.min(100, Math.round(operations.queuedInMemoryCount / operations.queueCapacity * 100))
}
