import { describe, expect, it } from 'vitest'
import type { Machine } from '../../types/machine'
import type { DetailRouteFormState } from './routeConfigDetail'
import { withLastStageMachine } from './processRouteConfigMachine'

describe('详情工艺路线机台默认值', () => {
  it('给最后新增阶段带出唯一默认机台', () => {
    const form = routeForm()

    const result = withLastStageMachine(form, [defaultSawMachine()])

    expect(result.stages[0]?.plan.machineUuid).toBe('machine-saw')
  })
})

function routeForm(): DetailRouteFormState {
  return {
    baseStageLevel: 1,
    firstOutputs: [],
    firstStepType: 1,
    stages: [{
      id: 'stage-2',
      inputOutputKeys: ['source-1'],
      plan: { processMode: 1, mainStepType: 1 },
      stageLevel: 2,
      stepType: 1,
    }],
  }
}

function defaultSawMachine(): Machine {
  return {
    uuid: 'machine-saw',
    machineName: '默认锯纸机',
    status: 1,
    capabilities: [{
      catalogUuid: 'saw',
      stepType: 1,
      processCode: 'SAW',
      processName: '锯纸',
      processCategory: 'PRODUCTION',
      defaultCapability: true,
      priority: 1,
    }],
  }
}
