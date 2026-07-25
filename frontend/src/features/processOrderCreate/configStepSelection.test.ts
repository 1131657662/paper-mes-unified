import { describe, expect, it } from 'vitest'
import {
  planBatchTargets,
  selectedConfigRoll,
  supportsRouteDesigner,
  supportsSinglePlanEditing,
} from './configStepSelection'
import type { RollDraft } from './types'

describe('加工方案操作范围', () => {
  it.each([
    { mode: 1, expected: true },
    { mode: 2, expected: true },
    { mode: 3, expected: false },
    { mode: 4, expected: false },
  ])('加工方式 $mode 的单道编辑权限为 $expected', ({ mode, expected }) => {
    expect(supportsSinglePlanEditing(mode)).toBe(expected)
  })

  it.each([
    { mode: 1, expected: true },
    { mode: 2, expected: false },
    { mode: 3, expected: false },
    { mode: 4, expected: false },
  ])('加工方式 $mode 的链式工艺入口权限为 $expected', ({ mode, expected }) => {
    expect(supportsRouteDesigner(mode)).toBe(expected)
  })

  it('批量目标排除链式路线和处理方式不兼容的母卷', () => {
    const selected = roll('source', 1, 2)
    const compatible = roll('compatible', 1, 2)
    const route = roll('route', 1, 2)
    const onSite = roll('on-site', 2, 2)

    const targets = planBatchTargets({
      checkedIds: [selected.localId, compatible.localId, route.localId, onSite.localId],
      locks: {},
      rolls: [selected, compatible, route, onSite],
      routePreviews: { route: { originalUuid: 'route', stages: [] } },
      selected,
    })

    expect(targets.map((item) => item.localId)).toEqual(['source', 'compatible'])
  })

  it('当前选中直发卷时回退到第一张可配置母卷', () => {
    const direct = roll('direct', 3)
    const standard = roll('standard', 1, 2)

    expect(selectedConfigRoll([direct, standard], direct.localId, {})).toBe(standard)
  })
})

function roll(localId: string, processMode: number, mainStepType?: number): RollDraft {
  return {
    gramWeight: 80,
    localId,
    mainStepType,
    originalWidth: 1000,
    paperName: '测试纸',
    pieceNum: 1,
    processMode,
    rollWeight: 100,
    uuid: localId,
  }
}
