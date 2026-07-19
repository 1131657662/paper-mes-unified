import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import { DownloadTaskCenterTrigger } from './DownloadTaskCenter'

describe('下载任务中心入口', () => {
  it('分别表达运行中与待处理任务数量', () => {
    const html = renderToStaticMarkup(
      <DownloadTaskCenterTrigger runningCount={2} unacknowledgedCount={3}
        error={false} onClick={() => undefined} />,
    )

    expect(html).toContain('下载任务中心，进行中 2 个，待处理 3 个')
    expect(html).toContain('is-running')
    expect(html).toContain('ant-badge-count')
    expect(html).toContain('title="3"')
    expect(html).not.toContain('is-error')
  })

  it('没有待处理任务时不显示数字徽标', () => {
    const html = renderToStaticMarkup(
      <DownloadTaskCenterTrigger runningCount={1} unacknowledgedCount={0}
        error={false} onClick={() => undefined} />,
    )

    expect(html).toContain('下载任务中心，进行中 1 个，待处理 0 个')
    expect(html).not.toContain('ant-badge-count')
  })

  it('汇总加载失败时展示明确的错误状态', () => {
    const html = renderToStaticMarkup(
      <DownloadTaskCenterTrigger runningCount={0} unacknowledgedCount={0}
        error onClick={() => undefined} />,
    )

    expect(html).toContain('下载任务中心，任务状态加载失败')
    expect(html).toContain('is-error')
  })
})
