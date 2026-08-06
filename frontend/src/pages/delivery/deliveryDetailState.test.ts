import { describe, expect, it } from 'vitest'
import type { DeliveryOrder } from '../../types/delivery'
import { resolveDeliveryOverview } from './deliveryDetailState'

describe('resolveDeliveryOverview', () => {
  it('treats a void order as released and not requiring sign-off', () => {
    const state = resolveDeliveryOverview(order(3))

    expect(state).toEqual({
      signState: 'NOT_REQUIRED',
      stockState: 'RELEASED',
      canSign: false,
      canEdit: false,
    })
  })

  it('prefers the backend projection when it is complete', () => {
    const state = resolveDeliveryOverview({
      ...order(1),
      signState: 'NOT_REQUIRED',
      stockState: 'RELEASED',
      canSign: false,
      canEdit: false,
    })

    expect(state.stockState).toBe('RELEASED')
    expect(state.canSign).toBe(false)
  })
})

function order(deliveryStatus: number): DeliveryOrder {
  return {
    uuid: 'delivery-1', deliveryNo: 'CK001', customerUuid: 'customer-1', customerName: '客户',
    deliveryDate: '2026-08-06', totalCount: 1, totalWeight: 1000, settleBlockAction: 0, deliveryStatus,
  }
}
