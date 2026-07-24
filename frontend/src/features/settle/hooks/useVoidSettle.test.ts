import { QueryClient } from '@tanstack/react-query'
import { describe, expect, it } from 'vitest'
import { queries } from '../../../queries'
import { invalidateVoidedSettle } from './useVoidSettle'

describe('作废结算后的加工单缓存', () => {
  it('只刷新返回的关联加工单及业务依赖', async () => {
    const queryClient = new QueryClient()
    const firstOrder = queries.processOrderDetail.detail('order-1').queryKey
    const secondOrder = queries.processOrderDetail.detail('order-2').queryKey
    const candidateKey = queries.settle.candidates({ current: 1, size: 20 }).queryKey
    ;[firstOrder, secondOrder, candidateKey].forEach((queryKey) => queryClient.setQueryData(queryKey, {}))

    await invalidateVoidedSettle(queryClient, ['order-1'])

    expect(queryClient.getQueryState(firstOrder)?.isInvalidated).toBe(true)
    expect(queryClient.getQueryState(secondOrder)?.isInvalidated).toBe(false)
    expect(queryClient.getQueryState(candidateKey)?.isInvalidated).toBe(true)
  })
})
