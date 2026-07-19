import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { ExportTaskOperations } from '../types/exportTask'
import { OperationsOverview } from './DownloadTaskOperations'

describe('下载任务运行状态视图', () => {
  it('展示全部核心指标和最早排队时间', () => {
    const html = renderToStaticMarkup(<OperationsOverview operations={operations()} onRefresh={() => undefined} />)

    expect(html).toContain('运行正常')
    expect(html).toContain('24 小时成功')
    expect(html).toContain('1 分 5 秒')
    expect(html).toContain('1.5 MB')
    expect(html).toContain('执行线程')
    expect(html).toContain('1 / 2')
    expect(html).toContain('共享目录正常')
    expect(html).toContain('剩余 50.0%')
    expect(html).toContain('内存队列')
    expect(html).toContain('7 / 50')
    expect(html).toContain('线程完成 12')
    expect(html).toContain('容量拒绝 0')
    expect(html).toContain('最早排队于 07-19 08:10:00')
    expect(html).toContain('更新于 08:30:00')
    expect(html).toContain('刷新运行状态和异常明细')
  })

  it('有停滞任务时显示需要关注和警示样式', () => {
    const html = renderToStaticMarkup(
      <OperationsOverview operations={operations({ staleRunningCount: 2 })} onRefresh={() => undefined} />,
    )

    expect(html).toContain('需要关注')
    expect(html).toContain('is-warning')
  })
})

function operations(overrides: Partial<ExportTaskOperations> = {}): ExportTaskOperations {
  return {
    queuedCount: 1, runningCount: 2, succeededLast24Hours: 12, failedLast24Hours: 0,
    staleRunningCount: 0, oldestQueuedAt: '2026-07-19T08:10:00', averageDurationSeconds: 65,
    asOf: '2026-07-19T08:30:00',
    storedFileBytes: 1572864, sseConnectionCount: 3,
    workerCount: 2, activeWorkerCount: 1, queuedInMemoryCount: 7, queueCapacity: 50,
    rejectedSubmissionCount: 0, completedExecutionCount: 12,
    storageStatus: 'READY', storageAvailable: true, storageWritable: true,
    storageFreeBytes: 8 * 1024 * 1024 * 1024, storageTotalBytes: 16 * 1024 * 1024 * 1024,
    storageFreePercent: 50, storageCheckedAt: '2026-07-19T08:30:00',
    ...overrides,
  }
}
