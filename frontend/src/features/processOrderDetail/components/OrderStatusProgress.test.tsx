import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import OrderStatusProgress from './OrderStatusProgress'

describe('OrderStatusProgress', () => {
  it('renders voided orders as an independent error terminal state', () => {
    const markup = renderToStaticMarkup(
      <OrderStatusProgress order={{ uuid: 'order-1', orderStatus: 6, voidReason: '客户取消' }} />,
    )

    expect(markup).toContain('已作废')
    expect(markup).toContain('客户取消')
    expect(markup).not.toContain('已结算')
  })
})
