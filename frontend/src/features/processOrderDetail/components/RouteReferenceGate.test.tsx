import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import RouteReferenceGate from './RouteReferenceGate'

describe('详情工艺配置基础资料门控', () => {
  it('资料失败时阻止配置内容并提供重试', () => {
    const markup = renderToStaticMarkup(
      <RouteReferenceGate isError isLoading={false} onRetry={() => undefined}>
        <button>保存工艺</button>
      </RouteReferenceGate>,
    )
    expect(markup).toContain('工艺基础资料加载失败')
    expect(markup).toContain('重新加载')
    expect(markup).not.toContain('保存工艺')
  })

  it('资料齐全后开放配置内容', () => {
    const markup = renderToStaticMarkup(
      <RouteReferenceGate isError={false} isLoading={false} onRetry={() => undefined}>
        <button>保存工艺</button>
      </RouteReferenceGate>,
    )
    expect(markup).toContain('保存工艺')
  })
})
