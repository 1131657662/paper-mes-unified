import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import PlanPreviewToolbar from './PlanPreviewToolbar'

describe('加工方案预览工具栏', () => {
  it('保存期间禁用刷新预览', () => {
    const markup = renderToStaticMarkup(
      <PlanPreviewToolbar
        configured={false}
        disabled
        onPreview={() => undefined}
      />,
    )

    expect(markup).toContain('刷新预览')
    expect(markup).toContain('disabled=""')
  })
})
