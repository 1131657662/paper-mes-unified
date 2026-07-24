import { describe, expect, it } from 'vitest'
import type { ProcessOrderDetailVO } from '../../types/processOrder'
import { buildExecutionSummary } from './orderExecutionUtils'

describe('加工单执行完整性', () => {
  it('仅附加工艺不要求锯纸或复卷主工序', () => {
    const detail: ProcessOrderDetailVO = {
      order: { uuid: 'order-1', orderStatus: 1 },
      originalRolls: [],
      rolls: [],
      steps: [{ uuid: 'service-1', originalUuid: 'roll-1', stepType: 3, isMain: 0 }],
      finishRolls: [{ uuid: 'finish-1', sourceType: 3, isSpare: 0, isRemain: 0, rollNoStatus: 1 }],
      rollProductions: [{
        originalUuid: 'roll-1',
        processMode: 4,
        steps: [{ uuid: 'service-1', originalUuid: 'roll-1', stepType: 3, isMain: 0 }],
        finishes: [],
      }],
    }

    const summary = buildExecutionSummary(detail)

    expect(summary.printableWarnings).toEqual([])
  })
})
