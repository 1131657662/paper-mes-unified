import { describe, expect, it } from 'vitest'
import type { ProcessPlanDTO } from '../../types/processOrder'
import { pendingConfigurationRolls } from './autoFinishConfigModel'
import type { RollDraft } from './types'

describe('pendingConfigurationRolls', () => {
  it('returns only standard rolls that still need an explicit configuration decision', () => {
    const rolls = [
      roll('manual'),
      roll('configured'),
      roll('direct', 3),
      roll('service', 4),
      roll('route'),
    ]

    const pending = pendingConfigurationRolls({
      configuredPlanIds: ['configured'],
      plans: Object.fromEntries(rolls.map((item) => [item.localId, plan(item)])),
      previews: { configured: { originalUuid: 'configured', ready: true } },
      routePreviews: { route: { originalUuid: 'route', stages: [] } },
      rolls,
    })

    expect(pending.map((item) => item.localId)).toEqual(['manual'])
  })

  it('keeps a saved id pending when the persisted preview is not ready', () => {
    const blocked = roll('blocked')

    const pending = pendingConfigurationRolls({
      configuredPlanIds: ['blocked'],
      plans: { blocked: plan(blocked) },
      previews: { blocked: { originalUuid: 'blocked', ready: false } },
      routePreviews: {},
      rolls: [blocked],
    })

    expect(pending).toEqual([blocked])
  })

  it('keeps a route roll pending until a saved route preview exists', () => {
    const route = roll('route')

    const pending = pendingConfigurationRolls({
      configuredPlanIds: [],
      plans: { route: plan(route) },
      previews: {},
      routePreviews: {},
      rolls: [route],
    })

    expect(pending).toEqual([route])
  })
})

function roll(localId: string, processMode = 1): RollDraft {
  return {
    localId,
    uuid: localId,
    paperName: localId,
    gramWeight: 80,
    originalWidth: 1000,
    rollWeight: 500,
    processMode,
    mainStepType: processMode === 1 ? 1 : undefined,
  }
}

function plan(source: RollDraft): ProcessPlanDTO {
  return {
    processMode: source.processMode ?? 1,
    mainStepType: source.mainStepType,
    finishSpecs: [{ itemType: 'FINISH', finishWidth: 900, count: 1 }],
  }
}
