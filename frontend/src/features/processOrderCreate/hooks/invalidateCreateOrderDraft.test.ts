import { QueryClient } from '@tanstack/react-query'
import { describe, expect, it } from 'vitest'
import { queries } from '../../../queries'
import { invalidateCreateOrderDraft } from './invalidateCreateOrderDraft'

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
})
