import { describe, expect, it } from 'vitest'
import { configKeysForRoll, isConfiguredPlanReady, reconcileConfiguredPlanIds } from './configuredPlanStatus'
import type { RollDraft } from './types'

describe('configured plan status', () => {
  it('keeps only saved plans whose backend preview is ready', () => {
    expect(reconcileConfiguredPlanIds(['old', 'blocked'], [
      { localId: 'ready', preview: { ready: true } },
      { localId: 'blocked', preview: { ready: false } },
      { localId: 'missing' },
    ])).toEqual(['old', 'ready'])
  })

  it('requires both a persisted id and a ready preview', () => {
    const roll = sampleRoll()
    expect(isConfiguredPlanReady(roll, ['roll-1'], { 'roll-1': { ready: false } })).toBe(false)
    expect(isConfiguredPlanReady(roll, [], { 'roll-1': { ready: true } })).toBe(false)
    expect(isConfiguredPlanReady(roll, ['roll-1'], { 'roll-1': { ready: true } })).toBe(true)
  })

  it('treats the local id and backend uuid as aliases for one roll', () => {
    const roll = { ...sampleRoll(), uuid: 'uuid-1' }

    expect(configKeysForRoll(roll)).toEqual(['roll-1', 'uuid-1'])
    expect(isConfiguredPlanReady(roll, ['uuid-1'], { 'uuid-1': { ready: true } })).toBe(true)
  })
})

function sampleRoll(): RollDraft {
  return {
    localId: 'roll-1',
    uuid: 'roll-1',
    paperName: 'test paper',
    gramWeight: 80,
    originalWidth: 1000,
    rollWeight: 500,
    processMode: 1,
    mainStepType: 2,
  }
}
