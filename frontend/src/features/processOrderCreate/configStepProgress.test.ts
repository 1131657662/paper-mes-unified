import { describe, expect, it } from 'vitest'
import { configStepProgress, configStepProgressText } from './configStepProgress'
import type { RollDraft } from './types'

describe('工艺配置进度', () => {
  it('只把持久化方案和无需配置母卷计入完成范围', () => {
    const rolls = [
      roll('saved-plan', 1),
      roll('preview-only', 2),
      roll('direct', 3),
      roll('saved-service', 4),
      roll('pending-service', 4),
      roll('merged-source', 1),
    ]

    const progress = configStepProgress({
      configuredPlanIds: ['saved-plan'],
      lockedRolls: {
        'merged-source': { consumeRatio: 100, ownerLabel: '母卷 1', ownerLocalId: 'saved-plan' },
      },
      previews: { 'saved-plan': { originalUuid: 'saved-plan', ready: true } },
      routePreviews: {},
      rolls,
      serviceConfigured: { 'saved-service': true, 'pending-service': false },
    })

    expect(progress).toEqual({ noConfigCount: 2, pendingCount: 2, savedCount: 2, totalCount: 6 })
    expect(configStepProgressText(progress)).toBe('共 6 卷 · 已保存 2 · 无需配置 2 · 待处理 2')
  })

  it('does not count a configured id with a blocked preview as saved', () => {
    const progress = configStepProgress({
      configuredPlanIds: ['blocked'],
      lockedRolls: {},
      previews: { blocked: { originalUuid: 'blocked', ready: false } },
      routePreviews: {},
      rolls: [roll('blocked', 1)],
      serviceConfigured: {},
    })

    expect(progress).toMatchObject({ pendingCount: 1, savedCount: 0 })
  })

  it('counts a persisted route as saved without a normal plan preview', () => {
    const progress = configStepProgress({
      configuredPlanIds: [],
      lockedRolls: {},
      previews: {},
      routePreviews: { route: { originalUuid: 'route', stages: [] } },
      rolls: [roll('route', 1)],
      serviceConfigured: {},
    })

    expect(progress).toMatchObject({ pendingCount: 0, savedCount: 1 })
  })
})

function roll(localId: string, processMode: number): RollDraft {
  return {
    gramWeight: 80,
    localId,
    originalWidth: 1000,
    paperName: '测试纸',
    pieceNum: 1,
    processMode,
    rollWeight: 100,
    uuid: localId,
  }
}
