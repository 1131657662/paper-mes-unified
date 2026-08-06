import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { SettleOrder } from '../../types/settle'
import SettleAmountOverview from './SettleAmountOverview'

describe('结算金额总览', () => {
  it('将聚合到账金额显示为实际到账', () => {
    const markup = renderToStaticMarkup(<SettleAmountOverview order={order()} />)

    expect(markup).toContain('实际到账 ¥7310.00')
    expect(markup).not.toContain('现金 ¥7310.00')
  })

  it('历史差异与已分解费用分开显示', () => {
    const markup = renderToStaticMarkup(
      <SettleAmountOverview
        details={[{ standardProcessAmount: 6510, pricingAdjustmentAmount: 0 } as never]}
        order={order({ historicalDifferenceAmount: 846 })}
      />,
    )

    expect(markup).toContain('已分解费用')
    expect(markup).toContain('未分解差异 ¥846.00')
    expect(markup).toContain('¥6510.00')
  })

  it('旧明细缺少标准费用字段时使用订单加工费回退', () => {
    const markup = renderToStaticMarkup(
      <SettleAmountOverview details={[{ standardProcessAmount: 0 } as never]} order={order()} />,
    )

    expect(markup).toContain('标准加工费 ¥6510.00')
  })
})

function order(overrides: Partial<SettleOrder> = {}): SettleOrder {
  return {
    uuid: 'settle-1',
    settleNo: 'JS202606300001',
    customerUuid: 'customer-1',
    customerName: '测试客户',
    settleType: 1,
    settleDate: '2026-06-30',
    sawAmount: 6510,
    rewindAmount: 0,
    serviceAmount: 0,
    extraAmount: 0,
    amountNoTax: 6510,
    taxAmount: 0,
    totalAmount: 7356,
    receivedAmount: 7356,
    cashReceivedAmount: 7310,
    scrapOffsetAmount: 0,
    discountAmount: 46,
    unreceivedAmount: 0,
    isInvoice: 2,
    settleStatus: 3,
    ...overrides,
  }
}
