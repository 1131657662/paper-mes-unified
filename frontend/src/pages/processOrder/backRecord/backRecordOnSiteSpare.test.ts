import { describe, expect, it } from 'vitest'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'
import { nextOnSiteFinishOutput, type OnSiteOutputRecordValues } from './backRecordOnSiteOutputModel'

describe('现场定尺备用号分配', () => {
  it('新增成品优先按顺序启用当前母卷的备用号', () => {
    const item = onSiteItem()
    const current: OnSiteOutputRecordValues[] = [{
      uuid: 'finish-planned',
      outputType: 'FINISH',
      originalUuid: 'roll-1',
    }]

    const output = nextOnSiteFinishOutput(item, current, 'roll-1')

    expect(output).toMatchObject({
      uuid: 'finish-spare-1',
      finishRollNo: 'A000002',
      isSpare: 1,
      originalUuid: 'roll-1',
    })
  })

  it('备用号用完后才创建无卷号的新成品', () => {
    const item = onSiteItem()
    const current: OnSiteOutputRecordValues[] = item.finishes.map(({ finish }) => ({
      uuid: finish.uuid,
      outputType: 'FINISH',
    }))

    const output = nextOnSiteFinishOutput(item, current, 'roll-1')

    expect(output).toEqual({ outputType: 'FINISH', originalUuid: 'roll-1' })
  })
})

function onSiteItem(): BackRecordWorkItem {
  const finishes = [
    { uuid: 'finish-planned', finishRollNo: 'A000001', isSpare: 0 },
    { uuid: 'finish-spare-1', finishRollNo: 'A000002', isSpare: 1 },
    { uuid: 'finish-spare-2', finishRollNo: 'A000003', isSpare: 1 },
  ]
  return {
    key: 'roll-roll-1',
    kind: 'roll',
    title: '母卷 1',
    roll: { uuid: 'roll-1', processMode: 2 },
    production: { originalUuid: 'roll-1' },
    rollProductions: [{
      originalUuid: 'roll-1',
      finishes: finishes.map((finish) => ({ uuid: finish.uuid, sources: [{ originalUuid: 'roll-1' }] })),
    }],
    isMergeGroup: false,
    sourceMode: 'linked',
    finishes: finishes.map((finish) => ({ finish, bindMode: 'linked' })),
  }
}
