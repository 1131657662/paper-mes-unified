import { describe, expect, it } from 'vitest'
import type { ProcessOrderDetailVO, RollProductionVO } from '../../../types/processOrder'
import { theoreticalBackRecordValues } from './backRecordTheoryFill'
import { sourceWeightSummary, workItemSourceRolls } from './backRecordSourceRolls'
import { buildBackRecordMetrics } from './backRecordMetrics'
import { buildBackRecordDTO, type BackRecordFormValues } from './backRecordUtils'
import { requiresMeasuredSourceWeights } from './backRecordWeightPolicy'
import { buildWorkItemMetrics } from './backRecordWorkbenchUtils'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'

describe('母卷重量回录策略', () => {
  it('合并复卷汇总全部来源卷而不是只读取主卷', () => {
    const item = mergeItem(1)
    const values = rollValues(600, 700, 700)

    const summary = sourceWeightSummary(item, values)
    const metrics = buildWorkItemMetrics(item, values)

    expect(workItemSourceRolls(item).map((roll) => roll.uuid)).toEqual(['roll-1', 'roll-2', 'roll-3'])
    expect(summary.completeTotal).toBe(2000)
    expect(metrics.rollActual).toBe(2000)
    expect(metrics.diff).toBe(0)
  })

  it('标准吨位复卷缺少任一来源实测时阻止完成', () => {
    const item = mergeItem(1)
    const metrics = buildWorkItemMetrics(item, rollValues(600, undefined, 700))

    expect(requiresMeasuredSourceWeights(item)).toBe(true)
    expect(metrics.rollActual).toBeUndefined()
    expect(metrics.missingRoll).toBe(true)
    expect(metrics.missingRolls).toBe(1)
  })

  it.each([2, 3, 4] as const)('计价模式 %s 不强制来源重量', (billingMode) => {
    const item = mergeItem(billingMode)
    const metrics = buildWorkItemMetrics(item, rollValues(undefined, undefined, undefined))

    expect(requiresMeasuredSourceWeights(item)).toBe(false)
    expect(metrics.missingRoll).toBe(false)
    expect(metrics.unverifiedRolls).toBe(3)
  })

  it('理论回填不会把标称重量伪装成实测重量', () => {
    const detail = detailFixture()

    const values = theoreticalBackRecordValues(detail)

    expect(values.rolls?.['roll-1']?.actualWeight).toBeUndefined()
  })

  it('汇总不会把 ESTIMATED 的正重量当作已复称', () => {
    const detail = detailFixture()
    detail.originalRolls = detail.originalRolls.map((roll) => ({
      ...roll,
      actualWeight: 1,
      weightStatus: 'ESTIMATED',
    }))

    const metrics = buildBackRecordMetrics(detail, { rolls: {} })

    expect(metrics.originalActualTotal).toBe(0)
    expect(metrics.originalWeightPending).toBe(true)
    expect(metrics.missingRollWeight).toBe(3)
  })

  it('合并组提交包含每个来源卷的独立实测重量', () => {
    const detail = detailFixture()
    const values = rollValues(600, 700, 700)

    const dto = buildBackRecordDTO(detail, values, undefined, undefined, {
      selectedRollUuids: new Set(['roll-1', 'roll-2', 'roll-3']),
    })

    expect(dto.rolls).toEqual([
      expect.objectContaining({ uuid: 'roll-1', actualWeight: 600 }),
      expect.objectContaining({ uuid: 'roll-2', actualWeight: 700 }),
      expect.objectContaining({ uuid: 'roll-3', actualWeight: 700 }),
    ])
  })
})

function mergeItem(billingMode: 1 | 2 | 3 | 4): BackRecordWorkItem {
  const productions: RollProductionVO[] = ['roll-1', 'roll-2', 'roll-3'].map((uuid, index) => ({
    originalUuid: uuid,
    rollWeight: 1,
    paperName: `原纸${index + 1}`,
    steps: index === 0 ? [{ uuid: 'step-1', originalUuid: 'roll-1', stepType: 2, billingMode }] : [],
    rewindParams: index === 0 ? [{ paramMode: 5 }] : [],
    finishes: index === 0 ? [{
      uuid: 'finish-1',
      sources: ['roll-1', 'roll-2', 'roll-3'].map((originalUuid) => ({ originalUuid })),
    }] : [],
  }))
  return {
    key: 'merge-roll-1',
    kind: 'roll',
    title: '合并复卷 3 卷',
    roll: { uuid: 'roll-1', processMode: 1 },
    production: productions[0],
    rollProductions: productions,
    isMergeGroup: true,
    sourceMode: 'linked',
    finishes: [{ finish: { uuid: 'finish-1', actualWeight: 2000 }, bindMode: 'linked' }],
  }
}

function rollValues(first?: number, second?: number, third?: number): BackRecordFormValues {
  return {
    rolls: {
      'roll-1': { actualWeight: first },
      'roll-2': { actualWeight: second },
      'roll-3': { actualWeight: third },
    },
  }
}

function detailFixture(): ProcessOrderDetailVO {
  const item = mergeItem(1)
  return {
    order: { uuid: 'order-1', version: 1 },
    originalRolls: ['roll-1', 'roll-2', 'roll-3'].map((uuid) => ({ uuid, rollWeight: 1 })),
    rolls: [],
    finishRolls: [{ uuid: 'finish-1', actualWeight: 2000 }],
    steps: [{ uuid: 'step-1', originalUuid: 'roll-1', stepType: 2, billingMode: 1 }],
    rollProductions: item.rollProductions,
  }
}
