import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import QueryLoadErrorAlert from './QueryLoadErrorAlert'

describe('数据加载失败提示', () => {
  it('明确区分接口失败与无数据并提供重试操作', () => {
    const markup = renderToStaticMarkup(
      <QueryLoadErrorAlert
        description="本次未取得数据，不能据此判断业务数据为空。"
        message="数据加载失败"
        onRetry={() => undefined}
      />,
    )

    expect(markup).toContain('数据加载失败')
    expect(markup).toContain('不能据此判断业务数据为空')
    expect(markup).toContain('重新加载')
  })
})
