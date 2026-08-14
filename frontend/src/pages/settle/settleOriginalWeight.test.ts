import { describe, expect, it } from 'vitest'
import type { SettlePrintLine } from '../../types/settle'
import { formatSettleOriginalWeight, summarizeSettleOriginalWeights } from './settleOriginalWeight'

describe('settlement original weight semantics', () => {
  it('formats unknown, reference, measured, and legacy line weights distinctly', () => {
    expect(formatSettleOriginalWeight(line({ originalWeightStatus: 'UNKNOWN', originalWeight: 1 })))
      .toBe('未知（待称重）')
    expect(formatSettleOriginalWeight(line({ originalWeightStatus: 'ESTIMATED', originalWeight: 1 })))
      .toBe('参考 1 kg（未实测）')
    expect(formatSettleOriginalWeight(line({ originalWeightStatus: 'MEASURED', originalWeight: 2000 })))
      .toBe('实测 2000 kg')
    expect(formatSettleOriginalWeight(line({ originalWeight: 3 }))).toBe('3 kg')
  })

  it('does not present a partial known weight as the total when one roll is unknown', () => {
    const summary = summarizeSettleOriginalWeights([
      line({ originalWeightStatus: 'MEASURED', originalWeight: 2000 }),
      line({ originalUuid: 'roll-2', originalWeightStatus: 'UNKNOWN', originalWeight: 99 }),
    ])

    expect(summary).toEqual({
      label: '原纸（含未知）',
      value: '2 卷 / 已知 2 t，1 卷待称重',
    })
  })

  it('labels an all-reference subtotal as not measured', () => {
    const summary = summarizeSettleOriginalWeights([
      line({ originalWeightStatus: 'ESTIMATED', originalWeight: 1 }),
      line({ originalUuid: 'roll-2', originalWeightStatus: 'ESTIMATED', originalWeight: 2 }),
    ])

    expect(summary).toEqual({ label: '原纸（参考）', value: '2 卷 / 0.003 t' })
  })
})

function line(overrides: Partial<SettlePrintLine>): SettlePrintLine {
  return {
    settleUuid: 'settle-1',
    orderUuid: 'order-1',
    orderNo: 'JG-1',
    originalUuid: 'roll-1',
    originalLabel: '母卷-1',
    ...overrides,
  }
}
