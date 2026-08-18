import { describe, expect, it } from 'vitest'
import { acceptedPaths, buildProcessAiFieldOptions, defaultAcceptedOptionIds } from './processAiFieldOptions'
import type { ProcessAiParseResult } from './types'

describe('processAiFieldOptions', () => {
  it('uses the same owner-based paths accepted by the backend', () => {
    const groups = buildProcessAiFieldOptions(rewindResult())
    const paths = acceptedPaths(groups, defaultAcceptedOptionIds(groups))

    expect(paths).toContain('/assignments/R1/processType')
    expect(paths).toContain('/assignments/R1/rewindIntent/diameterRule/parts')
    expect(paths).toContain('/assignments/R1/rewindIntent/core')
    expect(paths).toContain('/assignments/R1/ancillaryRequirements/label')
    expect(paths).not.toContain('/assignments/R1/rewindIntent/widthRule/unit')
  })

  it('keeps an ancillary-only selection independent from the main process', () => {
    const parsed = rewindResult()
    parsed.result.assignments[0]!.ancillaryRequirements = {
      packaging: { type: 'FILM', unit: 'PIECE', createsServiceStep: true },
    }
    parsed.compiled.packagingCandidates = [{
      ownerRollRef: 'R1', originalUuid: 'roll-1', coveredOriginalUuids: [], stepType: 4,
      packagingType: 'FILM', stepName: '包膜', billingBasis: 'PIECE', billingMode: 2,
      remark: '待确认',
    }]
    const groups = buildProcessAiFieldOptions(parsed)
    const packagingId = groups[0]!.options.find((option) => option.id.endsWith('/packaging'))!.id

    expect(acceptedPaths(groups, [packagingId])).toEqual([
      '/assignments/R1/ancillaryRequirements/packaging',
    ])
  })

  it('exposes a backend-resolved machine as its own selectable field', () => {
    const parsed = rewindResult()
    parsed.compiled.plans = [{
      ownerRollRef: 'R1', originalUuid: 'roll-1', coveredOriginalUuids: [],
      plan: { processMode: 1, mainStepType: 2, machineUuid: 'rewind-machine' }, preview: {},
    }]

    const machine = buildProcessAiFieldOptions(parsed)[0]!.options
      .find((option) => option.id.endsWith('/machine'))

    expect(machine).toMatchObject({
      label: '建议机台', paths: ['/assignments/R1/machineUuid'], category: 'PLAN',
    })
  })

  it('does not add required paths for an assignment excluded in step four', () => {
    const groups = buildProcessAiFieldOptions(rewindResult())

    expect(acceptedPaths(groups, [])).toEqual([])
  })
})

function rewindResult(): ProcessAiParseResult {
  return {
    conversationId: 'conversation-1', parseId: 'parse-1', parseRevision: 1,
    expectedVersion: 3, status: 'READY', expiresAt: '2026-08-16T12:00:00Z',
    result: {
      parseId: 'parse-1', schemaVersion: '1.0', unmappedText: [], conflicts: [],
      needsClarification: false, clarificationQuestions: [], assignments: [{
        sourceRollRefs: ['R1'], ownerRollRef: 'R1', coveredRollRefs: [], processType: 'REWIND',
        rewindIntent: {
          modeIntent: 'CHANGE_DIAMETER',
          diameterRule: { type: 'WEIGHT_SPLIT', parts: 2, ratios: [50, 50] },
          core: { value: 3, unit: 'inch', source: 'DEFAULT' },
          widthRule: { type: 'KEEP_SPEC', unit: 'mm' },
        },
        ancillaryRequirements: { label: { required: true, text: '贴标签' } },
        evidence: [{ field: 'diameterRule', text: '直径一分为二' }],
      }],
    },
    compiled: { eligible: true, plans: [], packagingCandidates: [], errors: [], warnings: [] },
  }
}
