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
      rolls,
      routes: { route: { originalUuid: 'route', stages: [] } },
    })

    expect(pending.map((item) => item.localId)).toEqual(['manual'])
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
