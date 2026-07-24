import { describe, expect, it } from 'vitest'
import type { RollDraft } from './types'
import {
  buildServiceStepBatch,
  countServiceApplyTargets,
  distributeFixedTotal,
  resolveServiceApplyTargets,
} from './serviceStepBatchModel'

describe('service step batch application', () => {
  it('distributes a fixed total without changing its cents total', () => {
    const amounts = distributeFixedTotal(100.01, 3)

    expect(amounts).toEqual([33.34, 33.34, 33.33])
    expect(amounts.reduce((sum, amount) => sum + amount, 0)).toBeCloseTo(100.01, 2)
  })

  it('copies a fixed amount to every roll when EACH scope is selected', () => {
    const steps = buildServiceStepBatch({
      originalUuid: 'source', stepType: 3, billingMode: 3, billingAmount: 7.5,
    }, ['roll-1', 'roll-2'], 'EACH')

    expect(steps.map((step) => step.billingAmount)).toEqual([7.5, 7.5])
  })

  it('copies standard pricing to every selected roll', () => {
    const steps = buildServiceStepBatch({
      originalUuid: 'source', stepType: 3, billingMode: 1, billingBasis: 'PIECE', unitPrice: 20,
    }, ['roll-1', 'roll-2'])

    expect(steps.map((step) => step.originalUuid)).toEqual(['roll-1', 'roll-2'])
    expect(steps.every((step) => step.unitPrice === 20)).toBe(true)
  })

  it('classifies existing same-type steps as updates instead of excluding them', () => {
    const result = resolveServiceApplyTargets({
      rolls: [roll('local-1', 'roll-1'), roll('local-2', 'roll-2'), roll('local-3', 'roll-3')],
      stepType: 3,
      steps: [
        { uuid: 'step-1', originalUuid: 'roll-1', stepType: 3, isMain: 0 },
        { uuid: 'step-2', originalUuid: 'roll-2', stepType: 3, isMain: 0 },
      ],
    })

    expect(result).toEqual({
      targetUuids: ['roll-1', 'roll-2', 'roll-3'],
      createCount: 1,
      updateCount: 2,
      excludedCount: 0,
    })
    expect(countServiceApplyTargets({ ...resultOptions(), stepType: 3 })).toBe(2)
  })

  it('includes processing rolls and excludes unsaved or direct-ship rolls', () => {
    const result = resolveServiceApplyTargets({
      rolls: [
        roll('local-1', 'roll-1'),
        { ...roll('local-2', 'roll-2'), processMode: 1 },
        { ...roll('local-3', 'roll-3'), processMode: 2 },
        { ...roll('local-4', 'roll-4'), processMode: 3 },
        roll('local-5'),
      ],
      stepType: 3,
    })

    expect(result).toEqual({
      targetUuids: ['roll-1', 'roll-2', 'roll-3'], createCount: 3, updateCount: 0, excludedCount: 2,
    })
  })

  it('returns zero targets until a process type is selected', () => {
    expect(countServiceApplyTargets({ rolls: [roll('local-1', 'roll-1')] })).toBe(0)
  })
})

function resultOptions() {
  return {
    rolls: [roll('local-1', 'roll-1'), roll('local-2', 'roll-2')],
    steps: [{ uuid: 'step-1', originalUuid: 'roll-1', stepType: 3, isMain: 0 }],
  }
}

function roll(localId: string, uuid?: string): RollDraft {
  return {
    localId,
    uuid,
    paperName: 'white card',
    gramWeight: 250,
    originalWidth: 1200,
    rollWeight: 1000,
    processMode: 4,
  }
}
