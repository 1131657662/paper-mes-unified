import { describe, expect, it } from 'vitest'
import type { PlanPreviewVO, ProcessPlanDTO } from '../../types/processOrder'
import type { CreateOrderDraftSnapshot } from './hooks/useCreateOrderDraftState'
import { capturePlanDirtySnapshot, restorePlanDirtySnapshots } from './planDirtySnapshot'
import type { RollDraft } from './types'

const rollA = roll('a', 'uuid-a')
const rollB = roll('b', 'uuid-b')
const savedA = plan(800)
const savedB = plan(900)
const previewA = preview('uuid-a', 800)
const previewB = preview('uuid-b', 900)

describe('逐卷加工方案脏状态快照', () => {
  it('只还原仍未保存的卷并保留其他卷的保存结果', () => {
    const baseline = state({
      configuredPlanIds: ['a', 'b'],
      plans: { a: savedA, b: savedB },
      previews: { a: previewA, b: previewB },
    })
    const dirtyA = capturePlanDirtySnapshot(baseline, 'a')
    const current = state({
      configuredPlanIds: ['b'],
      plans: { a: plan(700), b: plan(880) },
      previews: { a: preview('uuid-a', 700), b: preview('uuid-b', 880) },
    })

    const restored = restorePlanDirtySnapshots(current, [dirtyA])

    expect(restored.plans.a).toEqual(savedA)
    expect(restored.previews.a).toEqual(previewA)
    expect(restored.plans.b?.finishSpecs?.[0]?.finishWidth).toBe(880)
    expect(restored.configuredPlanIds).toEqual(expect.arrayContaining(['a', 'b']))
  })

  it('丢弃修改时移除基线中不存在的自动预览', () => {
    const baseline = state({ configuredPlanIds: [], plans: { a: savedA }, previews: {} })
    const dirtyA = capturePlanDirtySnapshot(baseline, 'a')
    const current = state({
      configuredPlanIds: [],
      plans: { a: plan(700) },
      previews: { a: preview('uuid-a', 700) },
    })

    const restored = restorePlanDirtySnapshots(current, [dirtyA])

    expect(restored.previews.a).toBeUndefined()
  })
})

function state(overrides: Partial<CreateOrderDraftSnapshot>): CreateOrderDraftSnapshot {
  return {
    configuredPlanIds: [], current: 3, plans: {}, previews: {}, routePreviews: {}, routes: {},
    rolls: [rollA, rollB], selectedId: 'a', ...overrides,
  }
}

function roll(localId: string, uuid: string): RollDraft {
  return { localId, uuid, paperName: '牛卡纸', gramWeight: 120, originalWidth: 1000,
    rollWeight: 500, pieceNum: 1, processMode: 1, mainStepType: 1 }
}

function plan(finishWidth: number): ProcessPlanDTO {
  return { processMode: 1, mainStepType: 1, finishSpecs: [{ finishWidth, count: 1 }] }
}

function preview(originalUuid: string, finishWidth: number): PlanPreviewVO {
  return { originalUuid, ready: true, summary: `门幅 ${finishWidth}` }
}
