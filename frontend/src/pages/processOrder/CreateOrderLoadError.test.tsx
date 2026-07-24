import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import { CreateOrderLoadError } from './CreateOrderPage'

describe('新建加工单加载失败状态', () => {
  it('草稿失败时说明不会覆盖草稿并提供重试', () => {
    const markup = renderToStaticMarkup(
      <CreateOrderLoadError kind="draft" onBack={() => undefined} onRetry={() => undefined} />,
    )

    expect(markup).toContain('加工单草稿加载失败')
    expect(markup).toContain('不会覆盖或保存草稿')
    expect(markup).toContain('重新加载')
  })

  it('基础资料失败时暂停录入避免错误默认值', () => {
    const markup = renderToStaticMarkup(
      <CreateOrderLoadError kind="reference" onBack={() => undefined} onRetry={() => undefined} />,
    )

    expect(markup).toContain('新建加工单基础资料加载失败')
    expect(markup).toContain('暂停录入')
  })
})
