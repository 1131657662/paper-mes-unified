import { describe, expect, it } from 'vitest'
import type { OriginalRoll, ProcessPlanDTO } from '../../types/processOrder'
import {
  buildDetailRouteDto,
  initialDetailRouteForm,
  routeOutputRowsForPlan,
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
  it('allocates saw output weight by width and preserves total weight', () => {
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
    const dto = buildDetailRouteDto(roll, form)

    expect(dto.originalUuid).toBe('roll-1')
    expect(dto.stages[0]).toMatchObject({
      stageLevel: 1,
      outputs: [expect.objectContaining({ finishWidth: 1000, estimateWeight: 100 })],
    })
  })
})
