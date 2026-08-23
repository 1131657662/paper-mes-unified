import { describe, expect, it } from 'vitest'
import type { OriginalRoll, ProcessPlanDTO } from '../../types/processOrder'
import {
  buildDetailRouteDto,
  initialDetailRouteForm,
  routeOutputRowsForPlan,
  STEP_TYPE_REWIND,
  STEP_TYPE_SAW,
} from './routeConfigDetail'

function originalRoll(overrides: Partial<OriginalRoll> = {}): OriginalRoll {
  return {
    uuid: 'roll-1',
    paperName: '白卡',
    gramWeight: 300,
    originalWidth: 1000,
    rollWeight: 100,
    pieceNum: 1,
    ...overrides,
  }
}

describe('routeConfigDetail', () => {
  it('allocates saw output weight by finish width and preserves total weight', () => {
    const plan: ProcessPlanDTO = {
      processMode: 1,
      mainStepType: STEP_TYPE_SAW,
      finishSpecs: [
        { finishWidth: 600, count: 1 },
        { finishWidth: 400, count: 1 },
      ],
    }

    const outputs = routeOutputRowsForPlan(2, [{
      estimateWeight: 100,
      finishWidth: 1000,
      label: '来源',
      outputKey: 'S1-F1',
      stageLevel: 1,
    }], plan)

    expect(outputs.map((output) => output.estimateWeight)).toEqual([60, 40])
    expect(outputs.reduce((sum, output) => sum + output.estimateWeight, 0)).toBe(100)
  })

  it('allocates a 2400mm mother roll as integer 621, 621, 620kg', () => {
    const plan: ProcessPlanDTO = {
      processMode: 1,
      mainStepType: STEP_TYPE_SAW,
      finishSpecs: [
        { finishWidth: 800, count: 1 },
        { finishWidth: 800, count: 1 },
        { finishWidth: 800, count: 1 },
      ],
    }

    const outputs = routeOutputRowsForPlan(2, [{
      estimateWeight: 1862,
      finishWidth: 2400,
      label: '来源',
      outputKey: 'S1-F1',
      stageLevel: 1,
    }], plan)

    expect(outputs.map((output) => output.estimateWeight)).toEqual([621, 621, 620])
    expect(outputs.reduce((sum, output) => sum + output.estimateWeight, 0)).toBe(1862)
  })

  it.each([
    { policy: 'LOSS' as const, expectedWeights: [60, 30], remain: false },
    { policy: 'ALLOCATE' as const, expectedWeights: [67, 33], remain: false },
    { policy: 'REMAINDER' as const, expectedWeights: [60, 30, 10], remain: true },
  ])('applies $policy to an unassigned saw width', ({ policy, expectedWeights, remain }) => {
    const plan: ProcessPlanDTO = {
      processMode: 1,
      mainStepType: STEP_TYPE_SAW,
      widthDifferencePolicy: policy,
      finishSpecs: [
        { finishWidth: 600, count: 1 },
        { finishWidth: 300, count: 1 },
      ],
    }

    const outputs = routeOutputRowsForPlan(2, [{
      estimateWeight: 100,
      finishWidth: 1000,
      label: '来源',
      outputKey: 'S1-F1',
      stageLevel: 1,
    }], plan)

    expect(outputs.map((output) => output.estimateWeight)).toEqual(expectedWeights)
    expect(outputs.reduce((sum, output) => sum + output.estimateWeight, 0))
      .toBe(expectedWeights.reduce((a, b) => a + b, 0))
    expect(outputs.some((output) => output.isRemain === 1)).toBe(remain)
  })

  it.each([
    { policy: 'LOSS' as const, expectedWeights: [60], expectedRemain: false },
    { policy: 'ALLOCATE' as const, expectedWeights: [100], expectedRemain: false },
    { policy: 'REMAINDER' as const, expectedWeights: [60, 40], expectedRemain: true },
  ])('applies $policy to an unassigned rewind width', ({ policy, expectedWeights, expectedRemain }) => {
    const plan: ProcessPlanDTO = {
      processMode: 1,
      mainStepType: STEP_TYPE_REWIND,
      rewindMode: 1,
      widthDifferencePolicy: policy,
      segments: [{
        segmentRatio: 1,
        layoutItems: [{ width: 600, quantity: 1, itemType: 'FINISH' }],
      }],
    }

    const outputs = routeOutputRowsForPlan(2, [{
      estimateWeight: 100,
      finishWidth: 1000,
      label: '来源',
      outputKey: 'S1-F1',
      stageLevel: 1,
    }], plan)

    expect(outputs.map((output) => output.estimateWeight)).toEqual(expectedWeights)
    expect(outputs.reduce((sum, output) => sum + output.estimateWeight, 0))
      .toBe(policy === 'LOSS' ? 60 : 100)
    expect(outputs.some((output) => output.isRemain === 1)).toBe(expectedRemain)
  })

  it('uses actual source weight first for mode five rewind routes', () => {
    const plan: ProcessPlanDTO = {
      processMode: 1,
      mainStepType: 2,
      rewindMode: 5,
      segments: [{
        sources: [
          { originalUuid: 'source-a', consumeRatio: 100 },
          { originalUuid: 'source-b', consumeRatio: 100 },
        ],
        layoutItems: [{ width: 1000, quantity: 1, itemType: 'FINISH' }],
      }],
    }

    const outputs = routeOutputRowsForPlan(2, [
      { estimateWeight: 621, actualWeight: 620, finishWidth: 1000, label: 'A', outputKey: 'source-a', stageLevel: 1 },
      { estimateWeight: 379, actualWeight: 381, finishWidth: 1000, label: 'B', outputKey: 'source-b', stageLevel: 1 },
    ], plan)

    expect(outputs[0]?.estimateWeight).toBe(1001)
    expect(outputs.reduce((sum, output) => sum + output.estimateWeight, 0)).toBe(1001)
  })

  it('uses a legacy empty ratio as the remaining source consumption', () => {
    const plan: ProcessPlanDTO = {
      processMode: 1,
      mainStepType: 2,
      rewindMode: 5,
      segments: [
        { sources: [{ originalUuid: 'source-a', consumeRatio: 50 }],
          layoutItems: [{ width: 1000, quantity: 1, itemType: 'FINISH' }] },
        { sources: [{ originalUuid: 'source-a' }],
          layoutItems: [{ width: 1000, quantity: 1, itemType: 'FINISH' }] },
      ],
    }

    const outputs = routeOutputRowsForPlan(2, [{
      estimateWeight: 100,
      finishWidth: 1000,
      label: 'A',
      outputKey: 'source-a',
      stageLevel: 1,
    }], plan)

    expect(outputs.map((output) => output.estimateWeight)).toEqual([50, 50])
  })

  it('uses the remaining ratio when explicit and legacy source rows share a segment', () => {
    const plan: ProcessPlanDTO = {
      processMode: 1,
      mainStepType: 2,
      rewindMode: 5,
      segments: [{
        sources: [
          { originalUuid: 'source-a', consumeRatio: 60 },
          { originalUuid: 'source-a' },
        ],
        layoutItems: [{ width: 1000, quantity: 1, itemType: 'FINISH' }],
      }],
    }

    const outputs = routeOutputRowsForPlan(2, [{
      estimateWeight: 100,
      finishWidth: 1000,
      label: 'A',
      outputKey: 'source-a',
      stageLevel: 1,
    }], plan)

    expect(outputs).toHaveLength(1)
    expect(outputs[0]?.estimateWeight).toBe(100)
  })

  it('rejects explicit source consumption totals above 100 percent', () => {
    const plan: ProcessPlanDTO = {
      processMode: 1,
      mainStepType: 2,
      rewindMode: 5,
      segments: [{
        sources: [
          { originalUuid: 'source-a', consumeRatio: 60 },
          { originalUuid: 'source-a', consumeRatio: 60 },
        ],
        layoutItems: [{ width: 1000, quantity: 1, itemType: 'FINISH' }],
      }],
    }

    expect(() => routeOutputRowsForPlan(2, [{
      estimateWeight: 100,
      finishWidth: 1000,
      label: 'A',
      outputKey: 'source-a',
      stageLevel: 1,
    }], plan)).toThrow('100%')
  })

  it('rejects mode five source consumption that does not close at 100 percent', () => {
    const plan: ProcessPlanDTO = {
      processMode: 1,
      mainStepType: 2,
      rewindMode: 5,
      segments: [{
        sources: [{ originalUuid: 'source-a', consumeRatio: 60 }],
        layoutItems: [{ width: 1000, quantity: 1, itemType: 'FINISH' }],
      }],
    }

    expect(() => routeOutputRowsForPlan(2, [{
      estimateWeight: 100,
      finishWidth: 1000,
      label: 'A',
      outputKey: 'source-a',
      stageLevel: 1,
    }], plan)).toThrow('100%')
  })

  it('creates an explicit trim output when saw widths do not consume the source width', () => {
    const plan: ProcessPlanDTO = {
      processMode: 1,
      mainStepType: STEP_TYPE_SAW,
      finishSpecs: [
        { finishWidth: 600, count: 1 },
        { finishWidth: 300, count: 1 },
      ],
    }

    const outputs = routeOutputRowsForPlan(2, [{
      estimateWeight: 100,
      finishWidth: 1000,
      label: '来源',
      outputKey: 'S1-F1',
      stageLevel: 1,
    }], plan)

    expect(outputs.at(-1)).toMatchObject({ finishWidth: 100, estimateWeight: 10, isRemain: 1 })
    expect(outputs.reduce((sum, output) => sum + output.estimateWeight, 0)).toBe(100)
  })

  it('uses measured stage output weight as the next route source', () => {
    const plan: ProcessPlanDTO = {
      processMode: 1,
      mainStepType: STEP_TYPE_SAW,
      finishSpecs: [{ finishWidth: 1000, count: 1 }],
    }

    const outputs = routeOutputRowsForPlan(2, [{
      estimateWeight: 621,
      finishWidth: 1000,
      label: '来源',
      outputKey: 'S1-F1',
      stageLevel: 1,
      actualWeight: 620,
    }], plan)

    expect(outputs[0]?.estimateWeight).toBe(620)
  })

  it('uses the original roll as the first output when no saved production exists', () => {
    const form = initialDetailRouteForm(originalRoll())

    expect(form.firstOutputs).toEqual([expect.objectContaining({
      estimateWeight: 100,
      finishWidth: 1000,
      outputKey: 'S1-F1',
      paperName: '白卡',
    })])
  })

  it('serializes configured stage outputs into the route preview DTO', () => {
    const roll = originalRoll()
    const form = initialDetailRouteForm(roll)
    const dto = buildDetailRouteDto(roll, form, 7)

    expect(dto.expectedVersion).toBe(7)
    expect(dto.originalUuid).toBe('roll-1')
    expect(dto.stages[0]).toMatchObject({
      stageLevel: 1,
      outputs: [expect.objectContaining({ finishWidth: 1000, estimateWeight: 100 })],
    })
  })
})
