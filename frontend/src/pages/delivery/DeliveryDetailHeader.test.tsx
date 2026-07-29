import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it, vi } from 'vitest'
import type { DeliveryOrder } from '../../types/delivery'
import DeliveryDetailHeader, { type DeliveryDetailHeaderActions } from './DeliveryDetailHeader'

describe('出库单详情操作区', () => {
  it('待出库单显示编辑出库信息入口', () => {
    const markup = renderToStaticMarkup(
      <DeliveryDetailHeader actions={actions()} order={order(1)} onBack={vi.fn()} />,
    )

    expect(markup).toContain('编辑出库信息')
  })

  it('已签收单不显示编辑出库信息入口', () => {
    const markup = renderToStaticMarkup(
      <DeliveryDetailHeader actions={actions()} order={order(2)} onBack={vi.fn()} />,
    )

    expect(markup).not.toContain('编辑出库信息')
    expect(markup).toContain('回退出库')
  })
})

function actions(): DeliveryDetailHeaderActions {
  const noop = vi.fn()
  return {
    canConfirm: true,
    canManage: true,
    cancelling: false,
    confirming: false,
    exporting: false,
    rollingBack: false,
    onAppend: noop,
    onCancel: noop,
    onConfirm: noop,
    onEdit: noop,
    onExport: noop,
    onPrint: noop,
    onRollback: noop,
  }
}

function order(deliveryStatus: number): DeliveryOrder {
  return {
    uuid: 'delivery-1',
    deliveryNo: 'CK001',
    customerUuid: 'customer-1',
    customerName: '拓翔',
    deliveryDate: '2026-07-29',
    totalCount: 1,
    totalWeight: 1000,
    settleBlockAction: 0,
    deliveryStatus,
  }
}
