import { QueryClient } from '@tanstack/react-query'
import { describe, expect, it } from 'vitest'
import { queries } from '../../../queries'
import { invalidateCreateOrderDraft, invalidateSubmittedProcessOrder } from './invalidateCreateOrderDraft'

describe('新建加工单草稿缓存失效', () => {
  it('保存步骤后同时失效当前草稿与草稿列表', async () => {
    const queryClient = new QueryClient()
    const detailKey = queries.createOrder.draft('draft-1').queryKey
    const listKey = queries.createOrder.drafts.queryKey
    queryClient.setQueryData(detailKey, { order: { uuid: 'draft-1' } })
    queryClient.setQueryData(listKey, [])

    await invalidateCreateOrderDraft(queryClient, 'draft-1')

    expect(queryClient.getQueryState(detailKey)?.isInvalidated).toBe(true)
    expect(queryClient.getQueryState(listKey)?.isInvalidated).toBe(true)
  })

  it('提交加工单后刷新草稿列表、仪表盘和报表', async () => {
    const queryClient = new QueryClient()
    const detailKey = queries.processOrderDetail.detail('draft-1').queryKey
    const keys = [
      queries.createOrder.drafts.queryKey,
      detailKey,
      queries.dashboard.overview.queryKey,
      queries.report.overview({}).queryKey,
    ]
    keys.forEach((queryKey) => queryClient.setQueryData(queryKey, {}))

    await invalidateSubmittedProcessOrder(queryClient, 'draft-1')

    keys.forEach((queryKey) => {
      expect(queryClient.getQueryState(queryKey)?.isInvalidated).toBe(true)
    })
  })
})
