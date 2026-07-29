import { describe, expect, it } from 'vitest'
import {
  planBatchSelectionReasons,
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

    expect(targets.map((item) => item.localId)).toEqual(['compatible'])
  })

  it('批量目标排除不同品名、克重和门幅的母卷', () => {
    const selected = roll('source', 1, 2)
    const differentPaper = { ...roll('paper', 1, 2), paperName: '不同品名' }
    const differentGram = { ...roll('gram', 1, 2), gramWeight: 100 }
    const differentWidth = { ...roll('width', 1, 2), originalWidth: 1090 }

    const targets = planBatchTargets({
      checkedIds: ['paper', 'gram', 'width'],
      locks: {},
      rolls: [selected, differentPaper, differentGram, differentWidth],
      routePreviews: {},
      selected,
    })

    expect(targets).toEqual([])
  })

  it('当前选中直发卷时回退到第一张可配置母卷', () => {
    const direct = roll('direct', 3)
    const standard = roll('standard', 1, 2)

    expect(selectedConfigRoll([direct, standard], direct.localId, {})).toBe(standard)
  })

  it('批量选择原因明确区分当前卷和不兼容卷', () => {
    const selected = roll('source', 1, 2)
    const different = { ...roll('different', 1, 2), originalWidth: 1300 }
    const reasons = planBatchSelectionReasons({
      locks: {}, rolls: [selected, different], routePreviews: {}, selected,
    })

    expect(reasons.source).toContain('当前母卷')
    expect(reasons.different).toContain('不同')
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
