import { describe, expect, it } from 'vitest'
import { classifyPlanBatchResult } from './planSaveResult'
import type { RollDraft } from './types'

describe('批量方案保存结果', () => {
  it('只把校验失败或缺少响应的母卷保留为待处理目标', () => {
    const result = classifyPlanBatchResult([roll('ready'), roll('blocked'), roll('missing')], {
      ready: { ready: true },
      blocked: { ready: false, errors: ['门幅超限'] },
    })

    expect(result).toEqual({
      appliedIds: ['ready', 'blocked'],
      failedIds: ['blocked', 'missing'],
      savedIds: ['ready'],
    })
  })
})

function roll(localId: string): RollDraft {
  return {
    localId, uuid: localId, paperName: '白卡', gramWeight: 300,
    originalWidth: 1200, rollWeight: 800, processMode: 1, mainStepType: 2,
  }
}
