import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import { SnapshotDiffLoadState } from './SnapshotDiffModal'
import type { SnapshotDiffVO } from '../../types/processOrder'

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

  it('历史下发快照明确标记未版本化来源', () => {
    const markup = renderToStaticMarkup(
      <SnapshotDiffLoadState isError={false} legacyUnversioned loading={false}
        onRetry={() => undefined} />,
    )
    expect(markup).toContain('下发快照来自 V3.53 前历史记录')
    expect(markup).toContain('未补造版本号、操作者或事件时间')
  })

  it('版本来源未加载成功时不展示缺少来源标记的差异', () => {
    const markup = renderToStaticMarkup(
      <SnapshotDiffLoadState isError loading={false} onRetry={() => undefined} />,
    )
    expect(markup).toContain('版本来源未成功加载')
    expect(markup).not.toContain('原纸快照差异')
  })

  it('展示每卷母卷的下发重量与完工实际重量差异', () => {
    const diff: SnapshotDiffVO = {
      rollDiffs: [{
        uuid: 'roll-1',
        rollNo: '01',
        printWeight: 1,
        finishWeight: 666.667,
        weightChanged: true,
      }],
    }
    const markup = renderToStaticMarkup(
      <SnapshotDiffLoadState diff={diff} isError={false} loading={false} onRetry={() => undefined} />,
    )
    expect(markup).toContain('重量(kg)')
    expect(markup).toContain('666.667')
  })
})
