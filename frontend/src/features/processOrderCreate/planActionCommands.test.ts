import type { SetStateAction } from 'react'
import { describe, expect, it, vi } from 'vitest'
import type { Machine } from '../../types/machine'
import type { PlanPreviewVO, ProcessPlanDTO } from '../../types/processOrder'
import { createPlanActionCommands, type PlanActionDependencies } from './planActionCommands'
import type { PlanCommandState } from './planActionResult'
import { PlanOperationTracker } from './planOperationTracker'
import type { RollDraft } from './types'
import type { VersionedWriteResult } from './draftWriteTypes'

vi.mock('antd', () => ({
  message: { success: vi.fn(), warning: vi.fn() },
}))

describe('加工方案异步响应提交', () => {
  it('保存期间出现新编辑时保留新值和未保存状态', async () => {
    const fixture = commandFixture(plan({ unitPrice: 153 }))
    const pending = deferred<VersionedWriteResult<PlanPreviewVO>>()
    fixture.deps.savePlan = () => pending.promise
    const commands = createPlanActionCommands(fixture.deps)

    const saving = commands.savePlan(fixture.roll, plan({ unitPrice: 153 }))
    commands.changePlan(fixture.roll.localId, plan({ unitPrice: 154 }))
    pending.resolve({ data: { ready: true }, recovered: false, version: 2 })
    const result = await saving

    expect(result).toMatchObject({ applied: false })
    expect(fixture.read().plans.a?.unitPrice).toBe(154)
    expect(fixture.read().configuredPlanIds).toEqual([])
    expect(fixture.read().previews).toEqual({})
  })

  it('旧预览响应不能覆盖更新后的编辑值', async () => {
    const fixture = commandFixture(plan({ unitPrice: 151 }))
    const pending = deferred<PlanPreviewVO>()
    fixture.deps.previewPlan = () => pending.promise
    const commands = createPlanActionCommands(fixture.deps)

    const previewing = commands.previewPlan(fixture.roll, plan({ unitPrice: 151 }))
    commands.changePlan(fixture.roll.localId, plan({ unitPrice: 152 }))
    pending.resolve({ ready: true, summary: '旧预览' })
    await previewing

    expect(fixture.read().plans.a?.unitPrice).toBe(152)
    expect(fixture.read().previews).toEqual({})
  })

  it('当前保存响应同步方案机台和就绪状态', async () => {
    const fixture = commandFixture(plan({ machineUuid: 'machine-new' }))
    fixture.deps.savePlan = async () => ({ data: { ready: true }, recovered: false, version: 2 })
    const commands = createPlanActionCommands(fixture.deps)

    const result = await commands.savePlan(fixture.roll, plan({ machineUuid: 'machine-new' }))

    expect(result).toMatchObject({ applied: true })
    expect(fixture.read().rolls[0]?.machineUuid).toBe('machine-new')
    expect(fixture.read().configuredPlanIds).toEqual(['a'])
  })

  it('批量保存只提交响应期间未被继续编辑的母卷', async () => {
    const fixture = commandFixture(plan({ unitPrice: 160 }))
    const second = { ...fixture.roll, localId: 'b', uuid: 'uuid-b' }
    fixture.deps.state.rolls = [fixture.roll, second]
    fixture.deps.state.configuredPlanIds = ['a', 'b']
    const pending = deferred<VersionedWriteResult<PlanPreviewVO[]>>()
    fixture.deps.savePlanBatch = () => pending.promise
    const commands = createPlanActionCommands(fixture.deps)

    const saving = commands.savePlanBatch([fixture.roll, second], plan({ unitPrice: 170 }))
    commands.changePlan(second.localId, plan({ unitPrice: 171 }))
    pending.resolve({
      data: [
        { originalUuid: fixture.roll.uuid, ready: true },
        { originalUuid: second.uuid, ready: true },
      ],
      recovered: false,
      version: 2,
    })
    const result = await saving

    expect(result).toEqual({ appliedIds: ['a'], failedIds: ['b'], savedIds: ['a'] })
    expect(fixture.read().plans.a?.unitPrice).toBe(170)
    expect(fixture.read().plans.b?.unitPrice).toBe(171)
    expect(fixture.read().configuredPlanIds).toEqual(['a'])
  })
})

function commandFixture(initialPlan: ProcessPlanDTO) {
  const roll: RollDraft = {
    localId: 'a', uuid: 'uuid-a', paperName: '白卡', gramWeight: 300,
    originalWidth: 1200, rollWeight: 800, processMode: 1, mainStepType: 2,
  }
  const store = createStateStore(roll, initialPlan)
  const deps: PlanActionDependencies = {
    defaultPlanOptions: {},
    machines: [machine('machine-new')],
    previewPlan: async () => ({ ready: true }),
    savePlan: async () => ({ data: { ready: true }, recovered: false, version: 2 }),
    savePlanBatch: async () => ({ data: [], recovered: false, version: 2 }),
    state: store.state,
    tracker: new PlanOperationTracker(),
  }
  return { deps, read: store.read, roll }
}

function createStateStore(roll: RollDraft, initialPlan: ProcessPlanDTO) {
  let plans = { [roll.localId]: initialPlan }
  let previews: Record<string, PlanPreviewVO> = {}
  let draftVersion = 1
  const state: PlanCommandState = {
    configuredPlanIds: [roll.localId],
    getDraftVersion: () => draftVersion,
    orderUuid: 'order-a',
    rolls: [roll],
    setConfiguredPlanIds: (update) => {
      state.configuredPlanIds = applyUpdate(state.configuredPlanIds, update)
    },
    setDraftVersion: (update) => { draftVersion = applyUpdate(draftVersion, update) },
    setPlans: (update) => { plans = applyUpdate(plans, update) },
    setPreviews: (update) => { previews = applyUpdate(previews, update) },
    setRolls: (update) => { state.rolls = applyUpdate(state.rolls, update) },
  }
  return {
    state,
    read: () => ({ configuredPlanIds: state.configuredPlanIds, plans, previews, rolls: state.rolls }),
  }
}

function applyUpdate<T>(current: T, update: SetStateAction<T>): T {
  return isUpdater(update) ? update(current) : update
}

function isUpdater<T>(update: SetStateAction<T>): update is (previous: T) => T {
  return typeof update === 'function'
}

function deferred<T>() {
  let resolve: (value: T) => void = () => undefined
  const promise = new Promise<T>((done) => { resolve = done })
  return { promise, resolve }
}

function plan(patch: Partial<ProcessPlanDTO>): ProcessPlanDTO {
  return { processMode: 1, mainStepType: 2, finishSpecs: [], segments: [], ...patch }
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
