import { describe, expect, it } from 'vitest'
import { nonDraftOrderUuid } from './draftAccess'

describe('加工单创建页访问边界', () => {
  it('草稿加工单继续停留在创建流程', () => {
    expect(nonDraftOrderUuid('draft-id', { order: { uuid: 'draft-id', orderStatus: 0 } })).toBeUndefined()
  })

  it('非草稿加工单返回详情页目标', () => {
    expect(nonDraftOrderUuid('issued-id', { order: { uuid: 'issued-id', orderStatus: 1 } }))
      .toBe('issued-id')
  })

  it('请求尚未完成时不提前跳转', () => {
    expect(nonDraftOrderUuid('loading-id', undefined)).toBeUndefined()
  })
})
