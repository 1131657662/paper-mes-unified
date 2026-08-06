import { describe, expect, it } from 'vitest'
import { hasDeliverySettlementRisk } from './settlementSemantics'

describe('delivery settlement risk semantics', () => {
  it('prefers the explicit risk state over the legacy flag', () => {
    expect(hasDeliverySettlementRisk({
      settlementRisk: true,
      settlementRiskState: 'NONE',
    })).toBe(false)
  })

  it('recognizes an unsettled cash risk state', () => {
    expect(hasDeliverySettlementRisk({ settlementRiskState: 'UNSETTLED_CASH' })).toBe(true)
  })

  it('supports legacy boolean responses during rolling upgrades', () => {
    expect(hasDeliverySettlementRisk({ settlementRisk: true })).toBe(true)
  })
})
