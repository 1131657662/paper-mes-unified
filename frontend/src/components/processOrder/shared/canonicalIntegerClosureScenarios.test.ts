import { describe, expect, it } from 'vitest'
import type {
  RollProductionVO,
  StageOutputVO,
} from '../../../types/processOrder'
import {
  canonicalFinishEstimateWeights,
  canonicalStageOutputWeights,
} from './canonicalEstimateWeight'

function production(
  overrides: Partial<RollProductionVO> = {},
): RollProductionVO {
  return {
    originalUuid: 'mother',
    actualWeight: 1501,
    actualWidth: 1550,
    originalWidth: 1550,
    mainStepType: 2,
    steps: [{ uuid: 'rewind', stageLevel: 1, stepType: 2, isMain: 1 }],
    ...overrides,
  }
}

function output(
  uuid: string,
  width: number,
  overrides: Partial<StageOutputVO> = {},
): StageOutputVO {
  return {
    uuid,
    stageLevel: 1,
    outputSort: 1,
    sourceStepType: 2,
    finishWidth: width,
    ...overrides,
  }
}

describe('整数预估重量闭合场景', () => {
  it('复卷直径一分二将1501kg分配为751kg和750kg', () => {
    const weights = canonicalStageOutputWeights(production(), [
      output('rewind-a', 1550),
      output('rewind-b', 1550, { outputSort: 2 }),
    ])

    const values = ['rewind-a', 'rewind-b'].map(
      (uuid) => weights.get(uuid) ?? 0,
    )
    expect(values).toEqual([751, 750])
    expect(values.reduce((sum, value) => sum + (value ?? 0), 0)).toBe(1501)
  })

  it('复卷成品计划保存为751kg和750kg后保持原值', () => {
    const weights = canonicalFinishEstimateWeights({
      production: production(),
      finishes: [
        {
          uuid: 'finish-a',
          finishRollNo: 'F001',
          finishWidth: 1550,
          estimateWeight: 751,
        },
        {
          uuid: 'finish-b',
          finishRollNo: 'F002',
          finishWidth: 1550,
          estimateWeight: 750,
        },
      ],
    })

    expect(['finish-a', 'finish-b'].map((uuid) => weights.get(uuid))).toEqual([
      751, 750,
    ])
  })

  it('复卷后的锯纸从父输出751kg继续整数闭合', () => {
    const chain = production({
      steps: [
        { uuid: 'rewind', stageLevel: 1, stepType: 2, isMain: 1 },
        { uuid: 'saw', stageLevel: 2, stepType: 1, isMain: 0 },
      ],
    })
    const weights = canonicalStageOutputWeights(chain, [
      output('rewind-a', 1550),
      output('rewind-b', 1550, { outputSort: 2 }),
      output('saw-a', 775, {
        stageLevel: 2,
        sourceStepType: 1,
        parentOutputUuid: 'rewind-a',
      }),
      output('saw-b', 775, {
        stageLevel: 2,
        outputSort: 2,
        sourceStepType: 1,
        parentOutputUuid: 'rewind-a',
      }),
    ])

    expect(
      ['rewind-a', 'rewind-b', 'saw-a', 'saw-b'].map((uuid) =>
        weights.get(uuid),
      ),
    ).toEqual([751, 750, 376, 375])
  })
})
