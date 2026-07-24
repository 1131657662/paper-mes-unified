import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it, vi } from 'vitest'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import PrintOnlySheet from './PrintOnlySheet'

vi.mock('./PrintPreviewSheet', () => ({ default: () => <section>车间加工单正文</section> }))

describe('车间加工单打印内容', () => {
  it('只打印加工单正文且不附加客户标签清单', () => {
    const markup = renderToStaticMarkup(
      <PrintOnlySheet copies={2} detail={detail()} version="ISSUED" />,
    )

    expect(markup.match(/车间加工单正文/g)).toHaveLength(2)
    expect(markup).not.toContain('成品客户标签清单')
  })
})

function detail(): ProcessOrderDetailVO {
  return { order: { uuid: 'order-1' }, originalRolls: [], rolls: [], finishRolls: [], steps: [] }
}
