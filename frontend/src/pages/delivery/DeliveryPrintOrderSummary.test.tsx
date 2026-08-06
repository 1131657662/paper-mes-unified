import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import DeliveryPrintOrderSummary from './DeliveryPrintOrderSummary'
import type { DeliveryPrintProjection } from './deliveryPrintProjection'

describe('DeliveryPrintOrderSummary', () => {
  it('shows the active sort order without adding controls', () => {
    const markup = renderToStaticMarkup(<DeliveryPrintOrderSummary projection={projection([
      { field: 'customerPaperName', direction: 'asc' },
      { field: 'customerDisplayWeight', direction: 'desc' },
      { field: 'finishRollNo', direction: 'asc' },
    ])} />)

    expect(markup).toContain('当前顺序：客户品名 ↑ · 客户重量 ↓ · 其余 1 项')
    expect(markup).not.toContain('<button')
  })

  it('identifies the original order when no sort is active', () => {
    const markup = renderToStaticMarkup(<DeliveryPrintOrderSummary projection={projection([])} />)

    expect(markup).toContain('当前顺序：原始单据顺序')
  })
})

function projection(sortChain: DeliveryPrintProjection['sortChain']): DeliveryPrintProjection {
  return { status: 'invalid', variant: 'customer', message: 'test', sortChain }
}
