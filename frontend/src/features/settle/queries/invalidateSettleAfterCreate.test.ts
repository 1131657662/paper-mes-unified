import { QueryClient } from '@tanstack/react-query'
import { describe, expect, it } from 'vitest'
import { queries } from '../../../queries'
import { invalidateSettleAfterCreate } from './invalidateSettleAfterCreate'

describe('创建结算单后的缓存失效范围', () => {
  it('只失效集合查询，不失效当前试算', async () => {
    const queryClient = new QueryClient()
    const candidates = queries.settle.candidates({ current: 1, size: 20 }).queryKey
    const list = queries.settle.list({ current: 1, size: 20 }).queryKey
    const summary = queries.settle.summary({ current: 1, size: 20 }).queryKey
    const quote = queries.settle.quoteByOrders({ orderUuids: ['order-1'] }).queryKey
    for (const key of [candidates, list, summary, quote]) queryClient.setQueryData(key, {})

    await invalidateSettleAfterCreate(queryClient)

    expect(queryClient.getQueryState(candidates)?.isInvalidated).toBe(true)
    expect(queryClient.getQueryState(list)?.isInvalidated).toBe(true)
    expect(queryClient.getQueryState(summary)?.isInvalidated).toBe(true)
    expect(queryClient.getQueryState(quote)?.isInvalidated).toBe(false)
  })
})
