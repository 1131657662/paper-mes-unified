import { describe, expect, it } from 'vitest'
import { parseProcessOrderListUrl, serializeProcessOrderListUrl } from './processOrderListUrlState'

describe('process order list URL state', () => {
  it('reads dashboard status and search filters from the URL', () => {
    const params = new URLSearchParams('orderStatus=3&keyword=JG2026&customerUuid=c-1&dateFrom=2026-07-01&dateTo=2026-07-22&page=2&size=50')

    const state = parseProcessOrderListUrl(params)

    expect(state).toEqual({
      filters: {
        keyword: 'JG2026', customerUuid: 'c-1',
        dateFrom: '2026-07-01', dateTo: '2026-07-22',
      },
      page: 2,
      pageSize: 50,
      quickStatus: '3',
    })
  })

  it('falls back safely when URL values are invalid', () => {
    const params = new URLSearchParams('orderStatus=9&page=-2&size=17&dateFrom=07-01-2026')

    const state = parseProcessOrderListUrl(params)

    expect(state).toEqual({
      filters: {
        keyword: undefined, customerUuid: undefined,
        dateFrom: undefined, dateTo: undefined,
      },
      page: 1,
      pageSize: undefined,
      quickStatus: 'all',
    })
  })

  it('serializes meaningful filters and navigation state', () => {
    const params = serializeProcessOrderListUrl({
      filters: { keyword: '白卡', dateFrom: '2026-07-01', dateTo: '2026-07-22' },
      page: 3,
      pageSize: 20,
      quickStatus: '4',
    })

    expect(params.toString()).toBe('orderStatus=4&page=3&size=20&keyword=%E7%99%BD%E5%8D%A1&dateFrom=2026-07-01&dateTo=2026-07-22')
  })
})
