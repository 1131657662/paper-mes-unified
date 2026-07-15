import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import FinishedProductsTable from './FinishedProductsTable'

describe('成品表格工具栏', () => {
  it('为显示切边开关提供可访问名称', () => {
    const markup = renderToStaticMarkup(<FinishedProductsTable rows={[]} />)

    expect(markup).toContain('aria-label="显示切边"')
  })
})
