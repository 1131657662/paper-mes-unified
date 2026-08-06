import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import DeliverySelectionHeaderActions from './DeliverySelectionHeaderActions'

describe('出库成品选择模式按钮', () => {
  it('普通模式提供专注选择入口', () => {
    const markup = render(false)

    expect(markup).toContain('aria-label="专注选择"')
    expect(markup).toContain('aria-pressed="false"')
  })

  it('专注模式提供返回完整表单入口', () => {
    const markup = render(true)

    expect(markup).toContain('aria-label="返回完整表单"')
    expect(markup).toContain('aria-pressed="true"')
  })
})

function render(expanded: boolean) {
  return renderToStaticMarkup(
    <DeliverySelectionHeaderActions
      expanded={expanded}
      finishes={[]}
      scope="product"
      selectedRowKeys={[]}
      totalCount={0}
      onScopeChange={() => undefined}
      onToggleExpanded={() => undefined}
    />,
  )
}
