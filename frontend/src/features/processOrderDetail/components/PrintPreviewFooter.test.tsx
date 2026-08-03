import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import { PrintFooter } from './PrintPreviewFooter'

describe('打印预览页脚', () => {
  it('保留操作、复核和完工信息，不展示班组长字段', () => {
    const markup = renderToStaticMarkup(<PrintFooter />)

    expect(markup).toContain('操作工：')
    expect(markup).toContain('复核人：')
    expect(markup).toContain('完工日期：')
    expect(markup).not.toContain('班组长')
  })
})
