import { describe, expect, it } from 'vitest'
import type { ExportTaskOperations } from '../../types/exportTask'
import {
  exportOperationsNeedAttention,
  exportExecutorQueueUsagePercent,
  formatOperationDuration,
  formatStoredFileBytes,
} from './exportTaskOperationsDisplay'

describe('导出任务运行状态展示', () => {
  it.each([
    { seconds: 0.4, expected: '< 1 秒' },
    { seconds: 45.2, expected: '45 秒' },
    { seconds: 60, expected: '1 分钟' },
    { seconds: 125, expected: '2 分 5 秒' },
  ])('格式化执行时长 $seconds', ({ seconds, expected }) => {
    expect(formatOperationDuration(seconds)).toBe(expected)
  })

  it.each([
    { bytes: 512, expected: '512 B' },
    { bytes: 1536, expected: '1.5 KB' },
    { bytes: 1572864, expected: '1.5 MB' },
    { bytes: 1610612736, expected: '1.5 GB' },
  ])('格式化文件占用 $bytes', ({ bytes, expected }) => {
    expect(formatStoredFileBytes(bytes)).toBe(expected)
  })

  it('存在失败或停滞任务时标记为需要关注', () => {
    expect(exportOperationsNeedAttention(operations({ failedLast24Hours: 1 }))).toBe(true)
    expect(exportOperationsNeedAttention(operations({ staleRunningCount: 1 }))).toBe(true)
    expect(exportOperationsNeedAttention(operations({ storageAvailable: false }))).toBe(true)
    expect(exportOperationsNeedAttention(operations({}))).toBe(false)
  })

  it('队列接近容量或发生拒绝时标记为需要关注', () => {
    expect(exportOperationsNeedAttention(operations({ queuedInMemoryCount: 40 }))).toBe(true)
    expect(exportOperationsNeedAttention(operations({ rejectedSubmissionCount: 1 }))).toBe(true)
    expect(exportOperationsNeedAttention(operations({ queuedInMemoryCount: 39 }))).toBe(false)
  })

  it('计算并限制内存队列占用比例', () => {
    expect(exportExecutorQueueUsagePercent(operations({ queuedInMemoryCount: 25 }))).toBe(50)
    expect(exportExecutorQueueUsagePercent(operations({ queuedInMemoryCount: 70 }))).toBe(100)
    expect(exportExecutorQueueUsagePercent(operations({ queueCapacity: 0 }))).toBe(0)
  })
})

function operations(overrides: Partial<ExportTaskOperations>): ExportTaskOperations {
  return {
    queuedCount: 0, runningCount: 0, succeededLast24Hours: 0, failedLast24Hours: 0,
    staleRunningCount: 0, averageDurationSeconds: 0, storedFileBytes: 0, sseConnectionCount: 0,
    workerCount: 2, activeWorkerCount: 0, queuedInMemoryCount: 0, queueCapacity: 50,
    rejectedSubmissionCount: 0, completedExecutionCount: 0,
    storageStatus: 'READY', storageAvailable: true, storageWritable: true,
    storageFreeBytes: 1_000_000, storageTotalBytes: 2_000_000,
    storageFreePercent: 50, storageCheckedAt: '2026-07-19T08:30:00',
    asOf: '2026-07-19T08:30:00',
    ...overrides,
  }
}
