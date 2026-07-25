import { QueryClient } from '@tanstack/react-query'
import { describe, expect, it } from 'vitest'
import { queries } from '../../../queries'
import { invalidateSettleFinancialChange } from './invalidateSettleFinancialChange'

describe('结算金额变更后的缓存失效', () => {
  it('刷新结算、仪表盘和报表读模型', async () => {
    const queryClient = new QueryClient()
    const keys = [
      queries.dashboard.overview.queryKey,
      queries.report.overview({}).queryKey,
      queries.settle.detail('settle-1').queryKey,
      queries.settle.detailHeader('settle-1').queryKey,
      queries.settle.receives('settle-1').queryKey,
      queries.settle.list({ current: 1, size: 20 }).queryKey,
      queries.settle.summary({ current: 1, size: 20 }).queryKey,
      queries.settle.collectionSummary({ current: 1, size: 20 }).queryKey,
    ]
    keys.forEach((queryKey) => queryClient.setQueryData(queryKey, {}))

    await invalidateSettleFinancialChange(queryClient, 'settle-1')

    keys.forEach((queryKey) => {
      expect(queryClient.getQueryState(queryKey)?.isInvalidated).toBe(true)
    })
  })
})
