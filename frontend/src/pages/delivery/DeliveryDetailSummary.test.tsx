import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { DeliveryOrder } from '../../types/delivery'
import { DeliveryOverview } from './DeliveryDetailSummary'

describe('DeliveryOverview', () => {
  it('shows the released inventory state for a void order', () => {
    const markup = renderToStaticMarkup(<DeliveryOverview order={voidOrder()} />)

    expect(markup).toContain('无需签收')
    expect(markup).toContain('库存已释放')
    expect(markup).toContain('作废后已释放全部库存占用')
    expect(markup).not.toContain('司机签收后扣减库存')
  })
})

function voidOrder(): DeliveryOrder {
  return {
    uuid: 'delivery-void', deliveryNo: 'CK001', customerUuid: 'customer-1', customerName: '客户',
    deliveryDate: '2026-08-06', totalCount: 1, totalWeight: 1000, settleBlockAction: 0, deliveryStatus: 3,
  }
}
