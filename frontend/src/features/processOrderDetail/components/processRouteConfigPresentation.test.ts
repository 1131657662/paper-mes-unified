import { describe, expect, it } from 'vitest'
import type { OriginalRoll } from '../../../types/processOrder'
import { formatRollWeight, rollLabel } from './processRouteConfigPresentation'

function roll(overrides: Partial<OriginalRoll> = {}): OriginalRoll {
  return {
    uuid: 'roll-1',
    originalWidth: 2400,
    rollWeight: 1862,
    pieceNum: 1,
    ...overrides,
  }
}

describe('process route configuration presentation', () => {
  it('does not render unknown mother-roll weight as zero', () => {
    const unknown = roll({ weightStatus: 'UNKNOWN', totalWeight: 1862 })

    expect(formatRollWeight(unknown)).toBe('待称重')
    expect(rollLabel(unknown)).toContain('待称重')
    expect(rollLabel(unknown)).not.toContain('0 kg')
  })

  it('renders known mother-roll estimates as whole kilograms', () => {
    expect(formatRollWeight(roll({ totalWeight: 1861.6 }))).toBe('1862 kg')
  })
})
