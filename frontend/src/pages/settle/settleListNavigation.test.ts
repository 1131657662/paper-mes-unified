import { describe, expect, it } from 'vitest'
import { settleListLocation, settleListReturnTarget } from './settleListNavigation'

describe('结算列表返回导航', () => {
  it('保留列表查询参数作为返回地址', () => {
    const from = settleListLocation('/settle-orders', '?queue=partial&page=2')
    expect(settleListReturnTarget({ from })).toBe('/settle-orders?queue=partial&page=2')
  })

  it.each([
    '/login',
    '/settle-orders/create',
    '/settle-orders/detail-1',
    '/settle-orders-archive?queue=pending',
    'https://example.com/settle-orders',
  ])('拒绝非结算列表来源地址：%s', (from) => {
    expect(settleListReturnTarget({ from })).toBe('/settle-orders')
  })
})
