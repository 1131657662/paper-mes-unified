import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import { ErrorBoundary } from './ErrorBoundary'

describe('页面错误边界', () => {
  it('正常状态保持业务内容不变', () => {
    const markup = renderToStaticMarkup(
      <ErrorBoundary mode="page"><div>业务页面</div></ErrorBoundary>,
    )

    expect(markup).toContain('业务页面')
    expect(markup).not.toContain('当前页面暂时无法显示')
  })
})
