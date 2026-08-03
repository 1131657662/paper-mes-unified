import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import OrderDetailHeader from './OrderDetailHeader'

describe('加工单详情页头打印状态', () => {
  it('只以打印确认次数判断是否已打印', () => {
    const markup = renderToStaticMarkup(
      <OrderDetailHeader
        order={{
          uuid: 'order-1',
          orderNo: 'JG202608030001',
          orderStatus: 2,
          printStatus: 1,
          printCount: 0,
        }}
      />,
    )

    expect(markup).toContain('已下发，未打印')
    expect(markup).not.toContain('已打印 0 次')
  })
})
