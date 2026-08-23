import { describe, expect, it } from 'vitest'
import type { PlanPreviewVO, ProcessPlanDTO } from '../../types/processOrder'
import type { RollDraft } from './types'
import { calculateRollWeightBalance } from './weightBalanceModel'

const roll: RollDraft = {
  localId: 'roll-1',
  paperName: '白卡',
  gramWeight: 265,
  originalWidth: 2353,
  rollWeight: 2285,
  pieceNum: 1,
  processMode: 1,
  mainStepType: 1,
}

const plan: ProcessPlanDTO = {
  processMode: 1,
  mainStepType: 1,
  widthDifferencePolicy: 'ALLOCATE',
}

describe('calculateRollWeightBalance', () => {
  it('treats allocated width difference as part of finished-product weight', () => {
    const preview: PlanPreviewVO = {
      ready: true,
      totalEstimateWeight: 2285,
      totalTrimWeight: 0,
      widthDifferencePolicy: 'ALLOCATE',
      widthDifference: 3,
      widthDifferenceWeight: 2.913,
    }

    const balance = calculateRollWeightBalance({ roll, rolls: [roll], plan, preview })

    expect(balance.status).toBe('balanced')
    expect(balance.finishWeight).toBe(2285)
    expect(balance.trimWeight).toBe(0)
    expect(balance.difference).toBe(0)
    expect(balance.detail).toContain('已分摊到所有实际产出件重量')
  })

  it('caps repeated merged-source consumption at the mother roll weight', () => {
    const mergedRoll = { ...roll, uuid: 'source-a', rollWeight: 1000, mainStepType: 2 }
    const mergedPlan: ProcessPlanDTO = {
      processMode: 1,
      mainStepType: 2,
      rewindMode: 5,
      segments: [
        { sources: [{ originalUuid: 'source-a', consumeRatio: 60 }] },
        { sources: [{ originalUuid: 'source-a', consumeRatio: 60 }] },
      ],
    }
    const preview: PlanPreviewVO = { ready: true, totalEstimateWeight: 1000, totalTrimWeight: 0 }

    const balance = calculateRollWeightBalance({
      roll: mergedRoll, rolls: [mergedRoll], plan: mergedPlan, preview,
    })

    expect(balance.inputWeight).toBe(1000)
    expect(balance.status).toBe('balanced')
  })
})
