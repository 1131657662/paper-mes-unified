import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { ExportTaskOperationsIssue } from '../types/exportTask'
import { OperationsIssuesView } from './DownloadTaskOperationIssues'

describe('导出异常任务明细', () => {
  it('展示停滞和失败任务的审计元数据', () => {
    const html = renderToStaticMarkup(<OperationsIssuesView issues={{
      staleTasks: [issue('stale-1', 2)],
      failedTasks: [issue('failed-1', 4)],
      asOf: '2026-07-19T08:30:00',
    }} />)

    expect(html).toContain('执行停滞')
    expect(html).toContain('24 小时失败')
    expect(html).toContain('操作员')
    expect(html).toContain('报表')
  })

  it('无异常任务时展示健康空状态', () => {
    const html = renderToStaticMarkup(<OperationsIssuesView issues={{
      staleTasks: [], failedTasks: [], asOf: '2026-07-19T08:30:00',
    }} />)

    expect(html).toContain('暂无停滞或近 24 小时失败任务')
  })
})

function issue(uuid: string, taskStatus: number): ExportTaskOperationsIssue {
  return {
    uuid, taskStatus, taskName: `任务-${uuid}`, requesterName: '操作员', moduleCode: 'report',
    errorMessage: taskStatus === 4 ? '导出任务执行失败，请重试或联系管理员' : undefined,
    createTime: '2026-07-19T08:00:00', heartbeatAt: '2026-07-19T08:10:00',
    completedAt: taskStatus === 4 ? '2026-07-19T08:20:00' : undefined,
  }
}
