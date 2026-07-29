import { renderToStaticMarkup } from 'react-dom/server'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { DeliveryOrder } from '../../../types/delivery'
import DeliveryOrderTable from './DeliveryOrderTable'

describe('出库单列表操作', () => {
  beforeEach(() => {
    vi.stubGlobal('localStorage', memoryStorage())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('待出库单显示编辑入口并明确收货客户字段', () => {
    const markup = renderTable(order(1))

    expect(markup).toContain('收货客户')
    expect(markup).toContain('编辑')
  })

  it('已出库单不显示编辑入口', () => {
    const markup = renderTable(order(2))

    expect(markup).not.toContain('编辑')
  })
})

function renderTable(data: DeliveryOrder) {
  return renderToStaticMarkup(
    <DeliveryOrderTable
      canManageDelivery
      data={[data]}
      loading={false}
      onConfirm={vi.fn()}
      onDetail={vi.fn()}
      onEdit={vi.fn()}
    />,
  )
}

function memoryStorage() {
  return {
    clear: () => undefined,
    getItem: () => null,
    key: () => null,
    length: 0,
    removeItem: () => undefined,
    setItem: () => undefined,
  }
}

function order(deliveryStatus: number): DeliveryOrder {
  return {
    customerName: '拓翔', customerUuid: 'customer-1', deliveryDate: '2026-07-29',
    deliveryNo: 'CK001', deliveryStatus, settleBlockAction: 0,
    totalCount: 1, totalWeight: 1000, uuid: 'delivery-1',
  }
}
