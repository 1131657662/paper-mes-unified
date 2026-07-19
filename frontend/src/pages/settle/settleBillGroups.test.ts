import { describe, expect, it } from 'vitest'
import { buildSettleBillGroups } from './settleBillGroups'
import type { SettlePrintLine } from '../../types/settle'

function line(overrides: Partial<SettlePrintLine> = {}): SettlePrintLine {
  return {
    settleUuid: 'settle-1',
    orderUuid: 'order-1',
    orderNo: 'JG-1',
    originalUuid: 'roll-1',
    originalLabel: '母卷-1',
    ...overrides,
  }
}

describe('buildSettleBillGroups', () => {
  it('aggregates standard, final, and adjustment amounts per order', () => {
    const groups = buildSettleBillGroups([
      line({ standardProcessAmount: 1200, pricingAdjustmentAmount: -190, processAmount: 1010 }),
      line({ originalUuid: 'roll-2', standardProcessAmount: 500, pricingAdjustmentAmount: 0, processAmount: 500 }),
    ])

    expect(groups).toHaveLength(1)
    expect(groups[0]).toMatchObject({
      standardProcessAmount: 1700,
      pricingAdjustmentAmount: -190,
      processAmount: 1510,
    })
  })

  it('falls back to final process amount for legacy lines without standard amount', () => {
    const [group] = buildSettleBillGroups([line({ processAmount: 300 })])

    expect(group).toBeDefined()
    if (!group) return
    expect(group.standardProcessAmount).toBe(300)
    expect(group.pricingAdjustmentAmount).toBe(0)
  })
})
