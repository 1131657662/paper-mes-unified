import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import {
  CustomerSpecRevisionDetailSection,
  CustomerSpecRevisionList,
} from './CustomerSpecRevisionHistoryDrawer'

describe('客户口径版本错误状态', () => {
  it('版本列表失败时不显示尚未发布', () => {
    const markup = renderToStaticMarkup(
      <CustomerSpecRevisionList isError loading={false} onRetry={() => undefined}
        onSelect={() => undefined} />,
    )
    expect(markup).toContain('客户口径版本加载失败')
    expect(markup).not.toContain('尚未发布客户口径版本')
  })

  it('版本明细失败时提供独立重试入口', () => {
    const markup = renderToStaticMarkup(
      <CustomerSpecRevisionDetailSection isError loading={false} onRetry={() => undefined} />,
    )
    expect(markup).toContain('客户口径版本明细加载失败')
    expect(markup).toContain('重新加载')
  })
})
