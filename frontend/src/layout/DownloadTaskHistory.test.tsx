import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { TaskHandlers } from './DownloadTaskList'
import { DownloadTaskHistoryResults } from './DownloadTaskHistory'

const handlers: TaskHandlers = {
  onDownload: () => undefined,
  onRetry: () => undefined,
  onCancel: () => undefined,
  onAcknowledge: () => undefined,
  onOpenSource: () => undefined,
  busy: () => undefined,
}

describe('下载任务结果区', () => {
  it('后台刷新时只标记结果区忙碌', () => {
    const html = renderResults(true)

    expect(html).toContain('download-task-history__results is-refreshing')
    expect(html).toContain('aria-busy="true"')
  })

  it('空闲状态不展示刷新效果', () => {
    const html = renderResults(false)

    expect(html).not.toContain('is-refreshing')
    expect(html).toContain('aria-busy="false"')
  })
})

function renderResults(isFetching: boolean): string {
  return renderToStaticMarkup(
    <DownloadTaskHistoryResults handlers={handlers} isFetching={isFetching}
      query={{ current: 1, size: 10 }} onPageChange={() => undefined} />,
  )
}
