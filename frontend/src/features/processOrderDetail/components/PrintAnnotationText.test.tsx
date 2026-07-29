import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import { PrintOrderAnnotation, PrintSpecification } from './PrintAnnotationText'

describe('加工单标注文字', () => {
  it('明细层单个克重标注省略单位', () => {
    const markup = renderToStaticMarkup(
      <PrintSpecification spec="涂布牛卡 / 245 g / 1600 mm" annotations={[{ field: 'gramWeight', value: '250' }]} />,
    )

    expect(markup).toContain('标注克重：250')
    expect(markup).not.toContain('250 g/m²')
  })

  it('整单层克重和门幅保留单位', () => {
    const markup = renderToStaticMarkup(
      <PrintOrderAnnotation annotations={[
        { field: 'gramWeight', value: '250' },
        { field: 'finishWidth', value: '1580' },
      ]} />,
    )

    expect(markup).toContain('整单成品标注：标注克重：250 g/m²；标注门幅：1580 mm')
  })
})
