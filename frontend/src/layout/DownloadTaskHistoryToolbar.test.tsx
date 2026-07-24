import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { ExportTaskHistoryQuery } from '../types/exportTask'
import DownloadTaskHistoryToolbar from './DownloadTaskHistoryToolbar'

const baseQuery: ExportTaskHistoryQuery = { current: 1, size: 10 }

describe('下载任务筛选工具栏', () => {
  it('存在筛选条件时明确限定批量处理范围', () => {
    const html = renderToolbar({ ...baseQuery, moduleCode: 'settle' })

    expect(html).toContain('标记筛选结果')
    expect(html).not.toContain('全部标记已处理')
  })

  it('非终态筛选下不展示批量处理操作', () => {
    const html = renderToolbar({ ...baseQuery, taskStatus: 2 })

    expect(html).not.toContain('标记筛选结果')
    expect(html).not.toContain('全部标记已处理')
  })

  it('无筛选条件时保留全量处理操作', () => {
    expect(renderToolbar(baseQuery)).toContain('全部标记已处理')
  })

  it('查询快照尚未返回时不允许批量处理', () => {
    expect(renderToolbar(baseQuery, '')).not.toContain('全部标记已处理')
  })

  it('筛选切换期间保留批量操作位置但禁止提交旧快照', () => {
    const html = renderToolbar(baseQuery, '2026-07-19T15:30:00', false)

    expect(html).toContain('全部标记已处理')
    expect(html).toContain('disabled=""')
  })
})

function renderToolbar(
  query: ExportTaskHistoryQuery,
  snapshotAt: string | undefined = '2026-07-19T15:30:00',
  snapshotCurrent = true,
): string {
  const client = new QueryClient()
  return renderToStaticMarkup(
    <QueryClientProvider client={client}>
      <DownloadTaskHistoryToolbar query={query} snapshot={snapshotAt ? {
        asOf: snapshotAt, current: snapshotCurrent, unacknowledgedCount: 3,
      } : undefined}
        onAttentionChange={() => undefined} onKeywordChange={() => undefined}
        onModuleChange={() => undefined} onOperationChange={() => undefined}
        onStatusChange={() => undefined} />
    </QueryClientProvider>,
  )
}
