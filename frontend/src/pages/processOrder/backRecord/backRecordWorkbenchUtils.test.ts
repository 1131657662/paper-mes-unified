import { describe, expect, it } from 'vitest'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import { buildBackRecordWorkbench } from './backRecordWorkbenchUtils'

describe('buildBackRecordWorkbench', () => {
  it('keeps finishes without source relations in the review pool', () => {
    const workbench = buildBackRecordWorkbench(detailWithoutFinishSources())

    const rollItems = workbench.items.filter((item) => item.kind === 'roll')
    const pool = workbench.items.find((item) => item.kind === 'pool')
    expect(rollItems.every((item) => item.finishes.length === 0)).toBe(true)
    expect(pool?.finishes.map(({ finish }) => finish.uuid)).toEqual(['finish-1', 'finish-2'])
  })

  it('uses the same stable roll number even when production rows are sorted differently', () => {
    const detail = detailWithoutFinishSources()
    detail.originalRolls[0]!.paperName = 'Z paper'
    detail.originalRolls[1]!.paperName = 'A paper'
    detail.rollProductions![0]!.paperName = 'Z paper'
    detail.rollProductions![1]!.paperName = 'A paper'

    const workbench = buildBackRecordWorkbench(detail)

    expect(workbench.items.filter((item) => item.kind === 'roll').map((item) => item.title))
      .toEqual(['母卷 2', '母卷 1'])
  })
})

function detailWithoutFinishSources(): ProcessOrderDetailVO {
  return {
    order: { uuid: 'order-1' },
    originalRolls: [
      { uuid: 'roll-1', processMode: 1, rowSort: 1 },
      { uuid: 'roll-2', processMode: 1, rowSort: 2 },
    ],
    rolls: [],
    finishRolls: [
      { uuid: 'finish-1', finishRollNo: 'A000001', rollNoStatus: 1, sourceType: 1 },
      { uuid: 'finish-2', finishRollNo: 'A000002', rollNoStatus: 1, sourceType: 1 },
    ],
    steps: [],
    rollProductions: [
      { originalUuid: 'roll-1', processMode: 1, finishes: [] },
      { originalUuid: 'roll-2', processMode: 1, finishes: [] },
    ],
  }
}
