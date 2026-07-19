import { renderToStaticMarkup } from 'react-dom/server'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import type { ExportTask } from '../types/exportTask'
import DownloadTaskList, { type TaskHandlers } from './DownloadTaskList'

const handlers: TaskHandlers = {
  onDownload: () => undefined,
  onRetry: () => undefined,
  onCancel: () => undefined,
  onAcknowledge: () => undefined,
  onOpenSource: () => undefined,
  busy: () => undefined,
}

function createTask(overrides: Partial<ExportTask> = {}): ExportTask {
  return {
    uuid: 'task-1',
    taskType: 'excel',
    moduleCode: 'settle',
    taskName: '结算单 JS202607190001',
    sourceUuid: 'settle-1',
    taskStatus: 3,
    progress: 100,
    fileName: '结算单_JS202607190001.xlsx',
    fileSize: 6350,
    createTime: '2026-07-19T10:00:00',
    expiresAt: '2026-07-20T09:30:00',
    downloadCount: 0,
    acknowledged: false,
    resourceAccessible: true,
    ...overrides,
  }
}

describe('下载任务列表', () => {
  it('成功且未下载的任务展示文件名和有效期', () => {
    const html = renderToStaticMarkup(<DownloadTaskList items={[createTask()]} handlers={handlers} />)

    expect(html).toContain('结算单_JS202607190001.xlsx')
    expect(html).toContain('尚未下载')
    expect(html).toContain('有效至 07-20 09:30')
  })

  it.each([
    { downloadCount: 1, label: '首次下载' },
    { downloadCount: 2, label: '重复下载' },
  ])('$label任务展示累计次数和最近下载时间', ({ downloadCount }) => {
    const html = renderToStaticMarkup(
      <DownloadTaskList items={[createTask({
        downloadCount,
        downloadedAt: '2026-07-19T11:20:30',
      })]} handlers={handlers} />,
    )

    expect(html).toContain(`已下载 ${downloadCount} 次`)
    expect(html).toContain('最近下载 07-19 11:20')
    expect(html).not.toContain('尚未下载')
  })

  it('旧任务缺少下载时间时不展示虚假时间', () => {
    const html = renderToStaticMarkup(
      <DownloadTaskList items={[createTask({ downloadCount: 2 })]} handlers={handlers} />,
    )

    expect(html).toContain('已下载 2 次')
    expect(html).not.toContain('最近下载')
  })

  it('失败任务达到上限后展示明确提示', () => {
    const html = renderToStaticMarkup(
      <DownloadTaskList items={[createTask({ taskStatus: 4, attemptCount: 3, maxAttempts: 3 })]}
        handlers={handlers} />,
    )

    expect(html).toContain('已达重试上限（3/3 次）')
  })

  it('过期任务使用重新生成动作名称', () => {
    const html = renderToStaticMarkup(
      <DownloadTaskList items={[createTask({ taskStatus: 6, attemptCount: 1, maxAttempts: 3 })]}
        handlers={handlers} />,
    )

    expect(html).toContain('重新生成')
  })

  it('详情导出任务提供来源单据链接', () => {
    const html = renderTask(createTask({ taskType: 'SETTLE_DETAIL', sourceUuid: 'settle-1' }))

    expect(html).toContain('href="/settle-orders/settle-1"')
    expect(html).toContain('查看来源：结算单 JS202607190001')
  })

  it('筛选快照导出不伪造来源链接', () => {
    const html = renderTask(createTask({ taskType: 'REPORT_FULL', sourceUuid: 'report' }))

    expect(html).not.toContain('download-task-center__source-link')
  })

  it('业务权限撤销后保留记录但隐藏来源和敏感操作', () => {
    const html = renderTask(createTask({
      taskType: 'SETTLE_DETAIL',
      resourceAccessible: false,
    }))

    expect(html).toContain('权限已变更')
    expect(html).not.toContain('download-task-center__source-link')
    expect(html).not.toContain('anticon-download')
  })
})

function renderTask(task: ExportTask): string {
  return renderToStaticMarkup(
    <MemoryRouter><DownloadTaskList items={[task]} handlers={handlers} /></MemoryRouter>,
  )
}
