import { describe, expect, it } from 'vitest'
import { processOrderListLocation, processOrderReturnTarget } from './processOrderNavigation'

describe('加工单导航上下文', () => {
  it('保留列表筛选、页码和页大小', () => {
    const location = processOrderListLocation(
      '/process-orders',
      '?keyword=JG202607230001&orderStatus=2&page=3&size=50',
    )

    expect(processOrderReturnTarget({ from: location }, '/process-orders/order-1')).toBe(location)
  })

  it('拒绝非加工单列表的返回地址', () => {
    expect(processOrderReturnTarget({ from: '/login' }, '/process-orders/order-1'))
      .toBe('/process-orders/order-1')
  })
})
