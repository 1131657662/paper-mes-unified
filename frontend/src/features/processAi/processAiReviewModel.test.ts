import { describe, expect, it } from 'vitest'
import type { ProcessPlanDTO } from '../../types/processOrder'
import {
  buildProcessAiRemarkReview,
  buildProcessAiReviewGroups,
  conflictingOptionIds,
} from './processAiReviewModel'
import type { ProcessAiParseResult } from './types'

describe('processAiReviewModel', () => {
  it('marks a locally changed diameter as a conflict and excludes only that field', () => {
    const groups = buildProcessAiReviewGroups(result(), current(plan(1300)))

    const diameter = groups[0]?.options.find((option) => option.id.endsWith('/diameter'))
    expect(diameter).toMatchObject({ conflict: true, baselineValue: '1200mm / 100%', currentValue: '1300mm / 100%' })
    expect(conflictingOptionIds(groups)).toContain(diameter?.id)
  })

  it('does not mark a local value as conflicting when it already equals the AI proposal', () => {
    const groups = buildProcessAiReviewGroups(result(), current(plan(1000)))

    expect(groups[0]?.options.find((option) => option.id.endsWith('/diameter'))?.conflict).toBe(false)
  })

  it('does not treat system defaults as manual changes without a saved plan baseline', () => {
    const parsed = result()
    parsed.baseline!.plans[0]!.plan = undefined

    const groups = buildProcessAiReviewGroups(parsed, current(plan(1300)))

    expect(groups[0]?.options.filter((option) => option.conflict)).toEqual([])
    expect(conflictingOptionIds(groups)).toEqual(new Set())
    expect(groups[0]?.options.find((option) => option.id.endsWith('/rewind-mode'))?.required).toBe(true)
    expect(groups[0]?.options.find((option) => option.id.endsWith('/diameter'))?.required).toBe(true)
  })

  it('labels trim layout items so they cannot look like finished widths', () => {
    const currentPlan = plan(1200)
    currentPlan.segments![0]!.layoutItems![1]!.itemType = 'TRIM'

    const groups = buildProcessAiReviewGroups(result(), current(currentPlan))

    expect(groups[0]?.options.find((option) => option.id.endsWith('/width'))?.currentValue)
      .toBe('1000x1 + 余料 1000x1')
  })

  it('labels saw trim items in the AI proposal', () => {
    const parsed = sawResult()

    const groups = buildProcessAiReviewGroups(parsed, current(sawPlan()))

    expect(groups[0]?.options.find((option) => option.id.endsWith('/saw'))?.aiValue)
      .toBe('1 刀；900x1 + 余料 100x1')
  })

  it('uses the current manual remark without appending the conversation again', () => {
    const review = buildProcessAiRemarkReview(result(), 'manual', 'cut\ncut\nlabel')

    expect(review.proposedValue).toBe('manual')
    expect(review.conflict).toBe(true)
  })

  it('uses the current conversation requirement when the manual remark is empty', () => {
    const review = buildProcessAiRemarkReview(result(), '', 'cut\nlabel')

    expect(review.proposedValue).toBe('cut\nlabel')
    expect(review.conflict).toBe(false)
  })
})

function current(currentPlan: ProcessPlanDTO) {
  return {
    rolls: [{ localId: 'local-1', uuid: 'roll-1', paperName: 'paper', gramWeight: 250, originalWidth: 2000 }],
    plans: { 'local-1': currentPlan },
  }
}

function result(): ProcessAiParseResult {
  const candidate = plan(1000)
  return {
    conversationId: 'conversation-1', parseId: 'parse-1', parseRevision: 1,
    expectedVersion: 7, status: 'READY', expiresAt: '2026-08-17T00:00:00Z',
    baseline: { remarkLong: 'cut', plans: [{
      ownerRollRef: 'R1', originalUuid: 'roll-1', processMode: 2,
      mainStepType: 2, route: false, plan: plan(1200),
    }] },
    result: {
      parseId: 'parse-1', schemaVersion: '1.0', unmappedText: [], conflicts: [],
      needsClarification: false, clarificationQuestions: [], assignments: [{
        sourceRollRefs: ['R1'], ownerRollRef: 'R1', coveredRollRefs: [], processType: 'REWIND',
        rewindIntent: { modeIntent: 'CHANGE_DIAMETER', diameterRule: {
          type: 'EXPLICIT', targetDiameter: { value: 1000, unit: 'mm' },
        }, widthRule: { type: 'EXPLICIT', values: [1000, 1000], unit: 'mm' } },
        evidence: [{ field: 'diameter', text: '直径1000' }],
      }],
    },
    compiled: { eligible: true, packagingCandidates: [], errors: [], warnings: [], plans: [{
      ownerRollRef: 'R1', originalUuid: 'roll-1', coveredOriginalUuids: [],
      plan: candidate, preview: {},
    }] },
  }
}

function sawResult(): ProcessAiParseResult {
  const parsed = result()
  parsed.baseline!.plans[0] = {
    ...parsed.baseline!.plans[0]!, mainStepType: 1, plan: sawPlan(),
  }
  parsed.result.assignments[0] = {
    sourceRollRefs: ['R1'], ownerRollRef: 'R1', coveredRollRefs: [], processType: 'SAW',
    sawIntent: { type: 'EXPLICIT_WIDTHS', widths: [900], unit: 'mm' },
    evidence: [{ field: 'sawIntent', text: '全部切900' }],
  }
  parsed.compiled.plans[0]!.plan = sawPlan()
  return parsed
}

function sawPlan(): ProcessPlanDTO {
  return {
    processMode: 1, mainStepType: 1, machineUuid: 'saw-machine', knifeCount: 1,
    finishSpecs: [
      { itemType: 'FINISH', finishWidth: 900, count: 1 },
      { itemType: 'TRIM', finishWidth: 100, count: 1 },
    ],
  }
}

function plan(diameter: number): ProcessPlanDTO {
  return {
    processMode: 2, mainStepType: 2, rewindMode: 2,
    segments: [{ segmentRatio: 100, targetDiameter: diameter, finishCoreDiameter: 3,
      layoutItems: [{ width: 1000, quantity: 1, itemType: 'FINISH' },
        { width: 1000, quantity: 1, itemType: 'FINISH' }] }],
  }
}
