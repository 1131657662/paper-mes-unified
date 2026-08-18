import { describe, expect, it } from 'vitest'
import type { ProcessPlanDTO } from '../../types/processOrder'
import { applyProcessAiConfirmation } from './applyProcessAiConfirmation'
import type { ProcessAiConfirmResponse } from './types'

describe('applyProcessAiConfirmation', () => {
  it('keeps the backend-saved saw plan, machine, trim and preview configured', () => {
    const result = applyProcessAiConfirmation({
      rolls: [{ localId: 'local-1', uuid: 'roll-1', paperName: '纸', gramWeight: 80,
        originalWidth: 1000, pieceNum: 1, processMode: 1, mainStepType: 2,
        machineUuid: 'rewind-machine' }],
      plans: { 'local-1': { processMode: 1, mainStepType: 2, finishSpecs: [] } },
      previews: {},
      configuredPlanIds: [],
      confirmation: sawConfirmation(),
    })

    expect(result.rolls[0]).toMatchObject({
      mainStepType: 1, processMode: 1, machineUuid: 'saw-machine',
    })
    expect(result.plans['local-1']).toMatchObject({
      knifeCount: 1, mainStepType: 1, machineUuid: 'saw-machine',
    })
    expect(result.plans['local-1']?.finishSpecs).toEqual([
      { itemType: 'FINISH', finishWidth: 900, count: 1 },
      { itemType: 'TRIM', finishWidth: 100, count: 1 },
    ])
    expect(result.previews['local-1']).toEqual({ ready: true })
    expect(result.configuredPlanIds).toEqual(['local-1'])
  })

  it('does not touch candidates whose assignment paths were not accepted', () => {
    const confirmation = sawConfirmation()
    confirmation.plans['roll-2'] = {
      ...confirmation.plans['roll-1']!, ownerRollRef: 'R2', originalUuid: 'roll-2',
    }
    const result = applyProcessAiConfirmation({
      rolls: [roll('local-1', 'roll-1'), roll('local-2', 'roll-2')],
      plans: { 'local-1': rewindPlan(), 'local-2': rewindPlan() }, previews: {},
      configuredPlanIds: ['local-2'], confirmation,
    })

    expect(result.rolls[1]?.mainStepType).toBe(2)
    expect(result.plans['local-2']?.mainStepType).toBe(2)
    expect(result.configuredPlanIds).toEqual(['local-2', 'local-1'])
  })

  it('creates an unsaved packaging draft only when its field was accepted', () => {
    const confirmation = sawConfirmation()
    confirmation.acceptedFieldPaths.push('/assignments/R1/ancillaryRequirements/packaging')
    confirmation.packagingCandidates.push({
      ownerRollRef: 'R1', originalUuid: 'roll-1', coveredOriginalUuids: [], stepType: 4,
      packagingType: 'FILM', stepName: '包膜', billingBasis: 'PIECE', serviceQuantity: 6,
      billingMode: 2, unitPrice: 20, remark: '包膜；保存前请确认',
    })

    const result = applyProcessAiConfirmation({
      rolls: [roll('local-1', 'roll-1')], plans: { 'local-1': rewindPlan() },
      previews: {}, configuredPlanIds: [], confirmation,
    })

    expect(result.packagingDrafts).toEqual([{
      parseId: 'parse-1', ownerRollRef: 'R1', values: expect.objectContaining({
        originalUuid: 'roll-1', stepType: 4, billingMode: 2,
        serviceQuantity: 6, unitPrice: 20, aiParseId: 'parse-1', aiOwnerRollRef: 'R1',
      }),
    }])
  })

  it('uses the backend-confirmed final plan instead of merging it again in the browser', () => {
    const confirmation = sawConfirmation()
    confirmation.acceptedFieldPaths = ['/assignments/R1/rewindIntent/core']
    confirmation.plans['roll-1']!.plan = confirmedRewindPlan()

    const result = applyProcessAiConfirmation({
      rolls: [roll('local-1', 'roll-1')],
      plans: { 'local-1': { processMode: 1, mainStepType: 2, rewindMode: 2,
        segments: [{ segmentRatio: 1, finishCoreDiameter: 3 }] } },
      previews: {}, configuredPlanIds: [], confirmation,
    })

    expect(result.plans['local-1']).toEqual(confirmedRewindPlan())
  })
})

function roll(localId: string, uuid: string) {
  return { localId, uuid, paperName: '纸', gramWeight: 80, originalWidth: 2000,
    pieceNum: 1, processMode: 1, mainStepType: 2 }
}

function rewindPlan() {
  return { processMode: 1, mainStepType: 2, finishSpecs: [] }
}

function confirmedRewindPlan(): ProcessPlanDTO {
  return {
    processMode: 1, mainStepType: 2, rewindMode: 3, allocationRule: 'WEIGHT_SPLIT',
    segments: [
      { segmentSort: 1, segmentRatio: 50, targetDiameter: 1200, finishCoreDiameter: 3,
        repeatCount: 1, layoutItems: [{ width: 1000, quantity: 1, itemType: 'FINISH' }] },
      { segmentSort: 2, segmentRatio: 50, targetDiameter: 1200, finishCoreDiameter: 3,
        repeatCount: 1, layoutItems: [{ width: 1000, quantity: 1, itemType: 'FINISH' }] },
    ],
  }
}

function sawConfirmation(): ProcessAiConfirmResponse {
  return {
    conversationId: 'conversation-1', parseId: 'parse-1', parseRevision: 1,
    expectedVersion: 3, nextVersion: 4, status: 'CONFIRMED', planHash: 'hash', warnings: [],
    acceptedFieldPaths: ['/assignments/R1/processType', '/assignments/R1/sawIntent/type',
      '/assignments/R1/sawIntent/knifeCount'],
    packagingCandidates: [],
    plans: {
      'roll-1': {
        ownerRollRef: 'R1', originalUuid: 'roll-1', coveredOriginalUuids: [], preview: { ready: true },
        plan: { processMode: 1, mainStepType: 1, machineUuid: 'saw-machine',
          knifeCount: 1, spareCount: 0, widthDifferencePolicy: 'REMAINDER',
          finishSpecs: [
            { itemType: 'FINISH', finishWidth: 900, count: 1 },
            { itemType: 'TRIM', finishWidth: 100, count: 1 },
          ] },
      },
    },
  }
}
