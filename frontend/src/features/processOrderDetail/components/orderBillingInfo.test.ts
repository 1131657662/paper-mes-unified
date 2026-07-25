import { describe, expect, it } from 'vitest'
import { buildBillingInfo } from './orderBillingInfo'

describe('加工单结算审计信息', () => {
  it('展示客户继承来源和客户版本', () => {
    const items = buildBillingInfo({ uuid: 'order-1', settleSource: 'INHERIT', settleCustomerVersion: 5 })

    expect(items).toContainEqual({ label: '结算来源', value: '跟随客户 · 客户版本 5' })
  })

  it('展示本单覆盖原因', () => {
    const items = buildBillingInfo({
      uuid: 'order-1', settleSource: 'OVERRIDE', settleCustomerVersion: 5,
      settleOverrideReason: '合同约定',
    })

    expect(items).toContainEqual({ label: '覆盖原因', value: '合同约定' })
  })

  it('不推断历史加工单的结算来源', () => {
    const items = buildBillingInfo({ uuid: 'order-1', settleType: 2 })

    expect(items).toContainEqual({ label: '结算来源', value: '历史快照（来源未记录）' })
  })
})
