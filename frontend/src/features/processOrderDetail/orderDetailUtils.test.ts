import { describe, expect, it } from 'vitest'
import type { ProcessOrderDetailVO, RollProductionVO } from '../../types/processOrder'
import {
  buildDetailMetrics,
  resolveFinishEstimateWeight,
  sumProductionEstimateWeight,
} from './orderDetailUtils'

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

  it('does not use a placeholder total for an unknown mother roll', () => {
    const detail: ProcessOrderDetailVO = {
      order: { uuid: 'order-unknown' },
      originalRolls: [{
        uuid: 'roll-unknown',
        weightStatus: 'UNKNOWN',
        totalWeight: 1862,
        rollWeight: 621,
        pieceNum: 3,
      }],
      rolls: [],
      finishRolls: [],
      steps: [],
    }

    expect(buildDetailMetrics(detail).totalOriginalWeight).toBe(0)
  })

  it('subtracts trim weight before filling missing product estimates', () => {
    const production: RollProductionVO = {
      originalUuid: 'roll-1',
      rollWeight: 1000,
      pieceNum: 1,
      finishes: [
        { uuid: 'finish-1', finishWidth: 500 },
        { uuid: 'finish-2', finishWidth: 500 },
        { uuid: 'trim-1', isRemain: 1, trimWeightShare: 100 },
      ],
    }

    expect(sumProductionEstimateWeight(production)).toBe(900)
  })

  it('rebalances legacy equal estimates to close the measured mother-roll weight', () => {
    const production: RollProductionVO = {
      originalUuid: 'roll-legacy',
      originalWidth: 2400,
      actualWeight: 1862,
      mainStepType: 1,
      finishes: [
        { uuid: 'finish-a', finishWidth: 800, estimateWeight: 621 },
        { uuid: 'finish-b', finishWidth: 800, estimateWeight: 621 },
        { uuid: 'finish-c', finishWidth: 800, estimateWeight: 621 },
      ],
    }

    const weights = (production.finishes ?? []).map((finish) => (
      resolveFinishEstimateWeight(finish, production.finishes ?? [], production)
    ))

    expect(weights).toEqual([621, 621, 620])
    expect(sumProductionEstimateWeight(production)).toBe(1862)
  })
})
