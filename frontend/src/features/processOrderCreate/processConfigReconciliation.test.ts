import { describe, expect, it } from 'vitest'
import type { Machine } from '../../types/machine'
import type { ProcessPlanDTO } from '../../types/processOrder'
import { reconcileProcessConfigAfterModeChange } from './processConfigReconciliation'
import type { RollDraft } from './types'

describe('加工方式变更后的工艺状态', () => {
  it('只清理加工方式或主工艺发生变化的母卷', () => {
    const rolls = [roll('a', 'uuid-a', 2), roll('b', 'uuid-b', 1)]
    const plans = { a: plan(2), b: plan(2) }
    const result = reconcileProcessConfigAfterModeChange({
      configuredPlanIds: ['a', 'b'],
      defaultPlanOptions: {},
      machines: [],
      plans,
      previews: { a: { ready: true }, b: { ready: true } },
      routePreviews: { 'uuid-a': { originalUuid: 'uuid-a', stages: [] }, 'uuid-b': { originalUuid: 'uuid-b', stages: [] } },
      routes: { 'uuid-a': { originalUuid: 'uuid-a', stages: [] }, 'uuid-b': { originalUuid: 'uuid-b', stages: [] } },
      rolls,
    })

    expect(result.configuredPlanIds).toEqual(['a'])
    expect(Object.keys(result.previews)).toEqual(['a'])
    expect(Object.keys(result.routes)).toEqual(['uuid-a'])
    expect(Object.keys(result.routePreviews)).toEqual(['uuid-a'])
    expect(result.plans.a).toMatchObject(plans.a!)
    expect(result.plans.b?.mainStepType).toBe(1)
  })

  it('替换不兼容机台时同步清除旧方案的已配置状态', () => {
    const source = roll('a', 'uuid-a', 2)
    const result = reconcileProcessConfigAfterModeChange({
      configuredPlanIds: ['a'],
      defaultPlanOptions: {},
      machines: [machine('rewind-only')],
      plans: { a: { ...plan(2), machineUuid: 'archived-machine' } },
      previews: { a: { ready: true } },
      routePreviews: {},
      routes: {},
      rolls: [source],
    })

    expect(result.configuredPlanIds).toEqual([])
    expect(result.previews).toEqual({})
    expect(result.plans.a?.machineUuid).toBe('rewind-only')
  })

  it('重新打开草稿后按母卷 UUID 清理已失效的保存状态和预览', () => {
    const result = reconcileProcessConfigAfterModeChange({
      configuredPlanIds: ['uuid-a', 'uuid-b'],
      defaultPlanOptions: {},
      machines: [],
      plans: { a: plan(2), b: plan(2) },
      previews: { 'uuid-a': { ready: true }, 'uuid-b': { ready: true } },
      routePreviews: {},
      routes: {},
      rolls: [roll('a', 'uuid-a', 2), roll('b', 'uuid-b', 1)],
    })

    expect(result.configuredPlanIds).toEqual(['uuid-a'])
    expect(Object.keys(result.previews)).toEqual(['uuid-a'])
  })
})

function roll(localId: string, uuid: string, mainStepType: number): RollDraft {
  return {
    localId, uuid, mainStepType, processMode: 1,
    paperName: '白卡', gramWeight: 300, originalWidth: 1200, rollWeight: 800,
  }
}

function plan(mainStepType: number): ProcessPlanDTO {
  return { processMode: 1, mainStepType, finishSpecs: [], segments: [] }
}

function machine(uuid: string): Machine {
  return {
    uuid, machineName: '复卷机', status: 1,
    capabilities: [{
      catalogUuid: 'rewind', stepType: 2, processCode: 'REWIND', processName: '复卷',
      processCategory: 'PRODUCTION', defaultCapability: false, priority: 100,
    }],
  }
}
