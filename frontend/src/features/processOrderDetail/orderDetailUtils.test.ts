import { describe, expect, it } from 'vitest'
import type { ProcessOrderDetailVO } from '../../types/processOrder'
import { buildDetailMetrics } from './orderDetailUtils'

describe('buildDetailMetrics', () => {
  it('uses measured mother roll weight after back-recording', () => {
    const detail: ProcessOrderDetailVO = {
      order: { uuid: 'order-1' },
      originalRolls: [
        { uuid: 'roll-1', rollWeight: 1, totalWeight: 1, actualWeight: 666.6 },
        { uuid: 'roll-2', rollWeight: 1, totalWeight: 1, actualWeight: 666.6 },
        { uuid: 'roll-3', rollWeight: 1, totalWeight: 1, actualWeight: 666.8 },
      ],
      rolls: [],
      finishRolls: [],
      steps: [],
    }

    expect(buildDetailMetrics(detail).totalOriginalWeight).toBe(2000)
  })
})
