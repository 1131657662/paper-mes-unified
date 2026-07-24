import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import CustomerRequirementState from './CustomerRequirementState'

describe('客户口径加载状态', () => {
  it('加载失败时不显示伪空态并保留重试和历史入口', () => {
    const markup = renderToStaticMarkup(
      <CustomerRequirementState canEdit data={undefined} isError loading={false}
        onEdit={() => undefined} onHistory={() => undefined} onRetry={() => undefined} />,
    )

    expect(markup).toContain('客户口径加载失败')
    expect(markup).toContain('不能据此判断客户规格与实物一致')
    expect(markup).toContain('重新加载')
    expect(markup).toContain('查看历史版本')
    expect(markup).not.toContain('暂无成品口径')
  })
})
