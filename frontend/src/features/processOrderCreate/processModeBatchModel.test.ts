import { describe, expect, it } from 'vitest'
import { applyProcessModeBatch } from './processModeBatchModel'
import type { RollDraft } from './types'

describe('加工方式批量模型', () => {
  it('支持把现场定尺锯纸应用到选中卷', () => {
    const rolls = [roll('selected'), roll('other')]

    const result = applyProcessModeBatch({
      checkedIds: ['other'], machines: [], mainStepType: 1, processMode: 2, rolls,
    })

    expect(result[0]).toEqual(rolls[0])
    expect(result[1]).toMatchObject({ localId: 'other', processMode: 2, mainStepType: 1 })
  })

  it('直发批量设置会清除主工艺和机台', () => {
    const rolls = [roll('selected')]

    const result = applyProcessModeBatch({
      checkedIds: ['selected'], machines: [], processMode: 3, rolls,
    })

    expect(result[0]).toMatchObject({ processMode: 3, mainStepType: undefined, machineUuid: undefined })
  })
})

function roll(localId: string): RollDraft {
  return {
    localId,
    uuid: `uuid-${localId}`,
    paperName: '白卡',
    gramWeight: 300,
    originalWidth: 1200,
    rollWeight: 800,
    processMode: 1,
    mainStepType: 2,
    machineUuid: 'machine-1',
  }
}
