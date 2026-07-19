import { describe, expect, it } from 'vitest'
import { parseSettleListUrl, serializeSettleListUrl } from './settleListUrlState'

describe('结算列表 URL 状态', () => {
  it('恢复催收队列、分页和筛选条件', () => {
    const params = new URLSearchParams(
      'view=collection&collectionQueue=reminded&page=3&size=50&keyword=JS001&customerUuid=c1&dateFrom=2026-07-01&dateTo=2026-07-19&settleType=2',
    )

    const state = parseSettleListUrl(params)

    expect(state).toMatchObject({
      collectionQueue: 'reminded', page: 3, pageSize: 50, viewMode: 'collection',
      filters: { customerUuid: 'c1', dateFrom: '2026-07-01', dateTo: '2026-07-19', keyword: 'JS001', settleType: 2 },
    })
  })

  it('序列化后保留有效工作台状态', () => {
    const params = serializeSettleListUrl({
      collectionQueue: 'overdue',
      filters: { customerUuid: 'c1', keyword: '弘丰拓' },
      page: 2,
      pageSize: 20,
      queueFilter: 'partial',
      viewMode: 'documents',
    })

    expect(params.toString()).toBe('queue=partial&page=2&size=20&customerUuid=c1&keyword=%E5%BC%98%E4%B8%B0%E6%8B%93')
  })
})
