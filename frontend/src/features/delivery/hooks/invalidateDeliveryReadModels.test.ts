import { QueryClient } from '@tanstack/react-query'
import { describe, expect, it } from 'vitest'
import { queries } from '../../../queries'
import { invalidateDeliveryReadModels } from './invalidateDeliveryReadModels'

describe('出库写入后的跨模块缓存失效', () => {
  it('刷新出库、客户口径、加工单和结算读模型', async () => {
    const queryClient = new QueryClient()
    const relatedKeys = deliveryDependentKeys()
    relatedKeys.forEach((queryKey) => queryClient.setQueryData(queryKey, {}))

    await invalidateDeliveryReadModels(queryClient)

    relatedKeys.forEach((queryKey) => {
      expect(queryClient.getQueryState(queryKey)?.isInvalidated).toBe(true)
    })
  })
})

function deliveryDependentKeys() {
  return [
    queries.delivery.detail('delivery-1').queryKey,
    queries.delivery.inventorySummary({}).queryKey,
    queries.deliveryCustomerSpec.current('delivery-1').queryKey,
    queries.processOrderDetail.detail('order-1').queryKey,
    queries.processOrderDetail.printView('order-1', 'FINISHED').queryKey,
    queries.processOrderDetail.snapshotDiff('order-1').queryKey,
    queries.settle.candidates({ current: 1, size: 20 }).queryKey,
    queries.settle.quoteByOrders({ orderUuids: ['order-1'] }).queryKey,
    queries.settle.quoteByMonth({
      customerUuid: 'customer-1', periodStart: '2026-07-01', periodEnd: '2026-07-31',
    }).queryKey,
  ]
}
