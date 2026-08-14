import { describe, expect, it } from 'vitest'
import type { RollDraft } from './types'
import { updateRollWeightStatus } from './rollWeightStatus'

describe('updateRollWeightStatus', () => {
  it('switches an estimated roll to unknown and clears the stale reference weight', () => {
    const [updated] = updateRollWeightStatus([roll()], 'roll-1', 'UNKNOWN')

    expect(updated).toMatchObject({ localId: 'roll-1', weightStatus: 'UNKNOWN' })
    expect(updated?.rollWeight).toBeUndefined()
  })

  it('switches an unknown roll to estimated without changing other rolls', () => {
    const source = [roll({ weightStatus: 'UNKNOWN', rollWeight: undefined }), roll({ localId: 'roll-2' })]

    const updated = updateRollWeightStatus(source, 'roll-1', 'ESTIMATED')

    expect(updated[0]?.weightStatus).toBe('ESTIMATED')
    expect(updated[1]).toBe(source[1])
  })
})

function roll(overrides: Partial<RollDraft> = {}): RollDraft {
  return {
    localId: 'roll-1',
    paperName: '测试纸',
    gramWeight: 80,
    originalWidth: 1000,
    rollWeight: 1,
    weightStatus: 'ESTIMATED',
    ...overrides,
  }
}
