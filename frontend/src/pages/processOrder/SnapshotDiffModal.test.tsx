import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import { SnapshotDiffLoadState } from './SnapshotDiffModal'

describe('加工单快照差异加载状态', () => {
  it('失败时只显示重试错误而不显示空差异表', () => {
    const markup = renderToStaticMarkup(
      <SnapshotDiffLoadState isError loading={false} onRetry={() => undefined} />,
    )
    expect(markup).toContain('快照差异加载失败')
    expect(markup).toContain('重新加载')
    expect(markup).not.toContain('原纸快照差异')
    expect(markup).not.toContain('成品快照差异')
  })
})
