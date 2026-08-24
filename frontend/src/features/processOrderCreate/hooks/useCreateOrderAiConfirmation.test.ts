import { describe, expect, it } from 'vitest'
import { aiConfirmationVersionMatches } from './useCreateOrderAiConfirmation'

describe('create order AI confirmation', () => {
  it('only accepts the response that advances the current draft by one version', () => {
    expect(aiConfirmationVersionMatches({ expectedVersion: 3, nextVersion: 4 }, 3)).toBe(true)
    expect(aiConfirmationVersionMatches({ expectedVersion: 2, nextVersion: 3 }, 3)).toBe(false)
    expect(aiConfirmationVersionMatches({ expectedVersion: 3, nextVersion: 5 }, 3)).toBe(false)
  })
})
