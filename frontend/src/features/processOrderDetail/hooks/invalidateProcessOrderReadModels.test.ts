import { QueryClient } from '@tanstack/react-query'
import { describe, expect, it } from 'vitest'
import { queries } from '../../../queries'
import {
  invalidateProcessOrderBusinessDependents,
  invalidateProcessOrderLocalReadModels,
  invalidateProcessOrderReadModels,
} from './invalidateProcessOrderReadModels'

describe('加工单读模型缓存失效', () => {
  it('刷新本单及下游结算出库视图且不影响其他加工单', async () => {
    const queryClient = new QueryClient()
    const currentKeys = relatedKeys('order-1')
    const downstreamKeys = dependentKeys()
    const allRelatedKeys = [...currentKeys, ...downstreamKeys]
    const unrelatedKey = queries.processOrderDetail.detail('order-2').queryKey
    allRelatedKeys.forEach((queryKey) => queryClient.setQueryData(queryKey, {}))
    queryClient.setQueryData(unrelatedKey, {})

    await invalidateProcessOrderReadModels(queryClient, 'order-1')

    currentKeys.forEach((queryKey) => {
      expect(queryClient.getQueryState(queryKey)?.isInvalidated).toBe(true)
    })
    downstreamKeys.forEach((queryKey) => {
      expect(queryClient.getQueryState(queryKey)?.isInvalidated).toBe(true)
    })
    expect(queryClient.getQueryState(unrelatedKey)?.isInvalidated).toBe(false)
  })

  it('本地备注刷新只使本单读模型失效', async () => {
    const queryClient = new QueryClient()
    const localKeys = relatedKeys('order-1')
    const businessKeys = dependentKeys()
    ;[...localKeys, ...businessKeys].forEach((queryKey) => queryClient.setQueryData(queryKey, {}))

    await invalidateProcessOrderLocalReadModels(queryClient, 'order-1')

    localKeys.forEach((queryKey) => {
      expect(queryClient.getQueryState(queryKey)?.isInvalidated).toBe(true)
    })
    businessKeys.forEach((queryKey) => {
      expect(queryClient.getQueryState(queryKey)?.isInvalidated).toBe(false)
    })
  })

  it('业务变更刷新结算和出库依赖但不刷新本单详情', async () => {
    const queryClient = new QueryClient()
    const localKeys = relatedKeys('order-1')
    const businessKeys = dependentKeys()
    ;[...localKeys, ...businessKeys].forEach((queryKey) => queryClient.setQueryData(queryKey, {}))

    await invalidateProcessOrderBusinessDependents(queryClient)

    localKeys.forEach((queryKey) => {
      expect(queryClient.getQueryState(queryKey)?.isInvalidated).toBe(false)
    })
    businessKeys.forEach((queryKey) => {
      expect(queryClient.getQueryState(queryKey)?.isInvalidated).toBe(true)
    })
  })
})

function relatedKeys(orderUuid: string) {
  return [
    queries.processOrderDetail.detail(orderUuid).queryKey,
    queries.processOrderDetail.printView(orderUuid, 'ISSUED').queryKey,
    queries.processOrderDetail.printView(orderUuid, 'FINISHED').queryKey,
    queries.processOrderDetail.snapshotDiff(orderUuid).queryKey,
    queries.finishCustomerSpec.current(orderUuid).queryKey,
  ]
}

function dependentKeys() {
  return [
    queries.settle.candidates({ current: 1, size: 20 }).queryKey,
    queries.settle.quoteByOrders({ orderUuids: ['order-1'] }).queryKey,
    queries.settle.quoteByMonth({
      customerUuid: 'customer-1', periodStart: '2026-07-01', periodEnd: '2026-07-31',
    }).queryKey,
    queries.delivery.availableFinishPage({ current: 1, size: 20, customerUuid: 'customer-1' }).queryKey,
    queries.delivery.inventoryCustomers({ current: 1, size: 20 }).queryKey,
    queries.delivery.inventoryFinishes({ current: 1, size: 20 }).queryKey,
    queries.delivery.inventoryOrderGroups({ current: 1, size: 20 }).queryKey,
    queries.delivery.inventorySummary({}).queryKey,
    queries.delivery.inventoryUnassigned({ current: 1, size: 20 }).queryKey,
  ]
}
