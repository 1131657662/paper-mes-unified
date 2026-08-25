import { describe, expect, it } from 'vitest'
import type { ProcessOrderDetailVO, RollProductionVO } from '../../../types/processOrder'
import { confirmedReferenceBackRecordValues, theoreticalBackRecordValues } from './backRecordTheoryFill'
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

  it('历史关系缺少消耗比例时按整卷来源计量', () => {
    const item = mergeItem(2)
    item.finishes = [{ finish: {
      uuid: 'finish-1', actualWeight: 1000,
      sources: [{ originalUuid: 'roll-1', consumeRatio: 50 }, { originalUuid: 'roll-1' }],
    }, bindMode: 'linked' }]

    const summary = sourceWeightSummary(item, rollValues(600, 700, 700))

    expect(summary.completeTotal).toBe(2000)
  })

  it('回录遇到来源消耗比例超过100%时阻止计算', () => {
    const item = mergeItem(2)
    item.finishes = [{ finish: {
      uuid: 'finish-1', actualWeight: 1000,
      sources: [{ originalUuid: 'roll-1', consumeRatio: 60 },
        { originalUuid: 'roll-1', consumeRatio: 60 }],
    }, bindMode: 'linked' }]

    expect(() => sourceWeightSummary(item, rollValues(600, 700, 700)))
      .toThrow('来源消耗比例合计不能超过100%')
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
    detail.originalRolls = detail.originalRolls.map((roll) => ({ ...roll, weightStatus: 'UNKNOWN' }))

    const values = theoreticalBackRecordValues(detail)

    expect(values.rolls?.['roll-1']?.actualWeight).toBeUndefined()
    expect(values.rolls?.['roll-1']?.weightEntryMode).toBeUndefined()
  })

  it('理论回填为无旧估重的余料预留母卷预算并保持整数闭合', () => {
    const detail: ProcessOrderDetailVO = {
      order: { uuid: 'order-1', version: 1 },
      originalRolls: [{ uuid: 'roll-1', actualWeight: 1862, actualWidth: 2400, weightStatus: 'MEASURED' }],
      rolls: [],
      finishRolls: [
        { uuid: 'finish-a', finishWidth: 800, isRemain: 0 },
        { uuid: 'finish-b', finishWidth: 800, isRemain: 0 },
        { uuid: 'trim', finishWidth: 800, isRemain: 1 },
      ],
      steps: [{ uuid: 'step-1', originalUuid: 'roll-1', stageLevel: 1, stepType: 1, isMain: 1 }],
      rollProductions: [{
        originalUuid: 'roll-1',
        actualWidth: 2400,
        actualWeight: 1862,
        mainStepType: 1,
        steps: [{ uuid: 'step-1', originalUuid: 'roll-1', stageLevel: 1, stepType: 1, isMain: 1 }],
        finishes: [
          { uuid: 'finish-a', finishWidth: 800, isRemain: 0 },
          { uuid: 'finish-b', finishWidth: 800, isRemain: 0 },
          { uuid: 'trim', finishWidth: 800, isRemain: 1 },
        ],
      }],
    }

    const values = theoreticalBackRecordValues(detail)
    const weights = ['finish-a', 'finish-b', 'trim'].map((uuid) => values.finishes?.[uuid]?.actualWeight ?? 0)

    expect(weights).toEqual([621, 620, 621])
    expect(weights.reduce((sum, weight) => sum + weight, 0)).toBe(1862)
  })

  it('理论回填保留已保存的复卷成品和余料整数计划', () => {
    const detail: ProcessOrderDetailVO = {
      order: { uuid: 'order-1', version: 1 },
      originalRolls: [{ uuid: 'roll-1', actualWeight: 2403, actualWidth: 1625, weightStatus: 'MEASURED' }],
      rolls: [],
      finishRolls: [
        { uuid: 'product-a', finishRollNo: 'F001', finishWidth: 1550, estimateWeight: 1146 },
        { uuid: 'trim-a', finishRollNo: 'F002', finishWidth: 75, estimateWeight: 56, isRemain: 1 },
        { uuid: 'product-b', finishRollNo: 'F003', finishWidth: 1550, estimateWeight: 1146 },
        { uuid: 'trim-b', finishRollNo: 'F004', finishWidth: 75, estimateWeight: 55, isRemain: 1 },
      ],
      steps: [{ uuid: 'step-1', originalUuid: 'roll-1', stageLevel: 1, stepType: 2, isMain: 1 }],
      rollProductions: [{
        originalUuid: 'roll-1', actualWidth: 1625, actualWeight: 2403, mainStepType: 2,
        steps: [{ uuid: 'step-1', originalUuid: 'roll-1', stageLevel: 1, stepType: 2, isMain: 1 }],
        finishes: [
          { uuid: 'product-a', finishRollNo: 'F001', finishWidth: 1550, estimateWeight: 1146 },
          { uuid: 'trim-a', finishRollNo: 'F002', finishWidth: 75, estimateWeight: 56, isRemain: 1 },
          { uuid: 'product-b', finishRollNo: 'F003', finishWidth: 1550, estimateWeight: 1146 },
          { uuid: 'trim-b', finishRollNo: 'F004', finishWidth: 75, estimateWeight: 55, isRemain: 1 },
        ],
      }],
    }

    const values = theoreticalBackRecordValues(detail)
    const weights = ['product-a', 'trim-a', 'product-b', 'trim-b']
      .map((uuid) => values.finishes?.[uuid]?.actualWeight)

    expect(weights).toEqual([1146, 56, 1146, 55])
    expect(weights.reduce<number>((sum, weight) => sum + (weight ?? 0), 0)).toBe(2403)
  })

  it('实测小数导致剩余重量非整数时不伪造成品预估', () => {
    const detail: ProcessOrderDetailVO = {
      order: { uuid: 'order-1', version: 1 },
      originalRolls: [{ uuid: 'roll-1', actualWidth: 1000, actualWeight: 1000, weightStatus: 'MEASURED' }],
      rolls: [],
      finishRolls: [
        { uuid: 'measured', finishWidth: 500, actualWeight: 333.4 },
        { uuid: 'unknown', finishWidth: 500 },
      ],
      steps: [{ uuid: 'step-1', originalUuid: 'roll-1', stageLevel: 1, stepType: 1, isMain: 1 }],
      rollProductions: [{
        originalUuid: 'roll-1',
        actualWidth: 1000,
        actualWeight: 1000,
        mainStepType: 1,
        steps: [{ uuid: 'step-1', originalUuid: 'roll-1', stageLevel: 1, stepType: 1, isMain: 1 }],
        finishes: [
          { uuid: 'measured', finishWidth: 500, actualWeight: 333.4 },
          { uuid: 'unknown', finishWidth: 500 },
        ],
      }],
    }

    const values = theoreticalBackRecordValues(detail)

    expect(values.finishes?.measured?.actualWeight).toBe(333.4)
    expect(values.finishes?.unknown?.actualWeight).toBeUndefined()
  })

  it('实测小数时清除未实测成品的历史整数估重', () => {
    const detail: ProcessOrderDetailVO = {
      order: { uuid: 'order-1', version: 1 },
      originalRolls: [{ uuid: 'roll-1', actualWidth: 1000, actualWeight: 1000, weightStatus: 'MEASURED' }],
      rolls: [],
      finishRolls: [
        { uuid: 'measured', finishWidth: 500, actualWeight: 333.4 },
        { uuid: 'unknown', finishWidth: 500, estimateWeight: 600 },
      ],
      steps: [{ uuid: 'step-1', originalUuid: 'roll-1', stageLevel: 1, stepType: 1, isMain: 1 }],
      rollProductions: [{
        originalUuid: 'roll-1', actualWidth: 1000, actualWeight: 1000, mainStepType: 1,
        steps: [{ uuid: 'step-1', originalUuid: 'roll-1', stageLevel: 1, stepType: 1, isMain: 1 }],
        finishes: [
          { uuid: 'measured', finishWidth: 500, actualWeight: 333.4 },
          { uuid: 'unknown', finishWidth: 500, estimateWeight: 600 },
        ],
      }],
    }

    const values = theoreticalBackRecordValues(detail)

    expect(values.finishes?.unknown?.actualWeight).toBeUndefined()
  })

  it('理论回填在分摊模式下用不完整旧计划的余料预估保留母卷预算', () => {
    const detail: ProcessOrderDetailVO = {
      order: { uuid: 'order-1', version: 1 },
      originalRolls: [{ uuid: 'roll-1', actualWeight: 1000, actualWidth: 1000, weightStatus: 'MEASURED' }],
      rolls: [],
      finishRolls: [
        { uuid: 'finish-a', finishWidth: 600, isRemain: 0 },
        { uuid: 'trim', finishRollNo: 'T001', finishWidth: 100, isRemain: 1, estimateWeight: 400 },
      ],
      steps: [{ uuid: 'step-1', originalUuid: 'roll-1', stageLevel: 1, stepType: 1,
        isMain: 1, widthDifferencePolicy: 'ALLOCATE' }],
      rollProductions: [{
        originalUuid: 'roll-1', actualWidth: 1000, actualWeight: 1000, mainStepType: 1,
        steps: [{ uuid: 'step-1', originalUuid: 'roll-1', stageLevel: 1, stepType: 1,
          isMain: 1, widthDifferencePolicy: 'ALLOCATE' }],
        finishes: [
          { uuid: 'finish-a', finishWidth: 600, isRemain: 0 },
          { uuid: 'trim', finishRollNo: 'T001', finishWidth: 100, isRemain: 1, estimateWeight: 400 },
        ],
      }],
    }

    const values = theoreticalBackRecordValues(detail)

    expect(values.finishes?.['finish-a']?.actualWeight).toBe(900)
    expect(values.finishes?.['trim']?.actualWeight).toBe(100)
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

  it('preserves stored estimated weight as provisional during theory fill', () => {
    const detail = detailFixture()
    detail.originalRolls = detail.originalRolls.map((roll) => ({
      ...roll,
      actualWeight: 700,
      weightStatus: 'ESTIMATED',
    }))

    const values = theoreticalBackRecordValues(detail)

    expect(values.rolls?.['roll-1']?.actualWeight).toBe(700)
    expect(values.rolls?.['roll-1']?.weightEntryMode).toBe('USER_ESTIMATE')
  })

  it('marks a positive reference weight as explicitly confirmed', () => {
    const values = confirmedReferenceBackRecordValues(detailFixture())

    expect(values.rolls?.['roll-1']).toEqual(expect.objectContaining({
      actualWeight: 1,
      weightEntryMode: 'CONFIRM_REFERENCE',
    }))
  })

  it('keeps an unknown source roll unconfirmed during confirmed reference fill', () => {
    const detail = detailFixture()
    const firstRoll = detail.originalRolls[0]!
    detail.originalRolls[0] = { ...firstRoll, weightStatus: 'UNKNOWN' }

    const values = confirmedReferenceBackRecordValues(detail)

    expect(values.rolls?.['roll-1']?.actualWeight).toBeUndefined()
    expect(values.rolls?.['roll-1']?.weightEntryMode).toBeUndefined()
  })

  it('allows confirmed reference weights to satisfy standard merged rewind metrics', () => {
    const item = mergeItem(1)
    const values = {
      rolls: {
        'roll-1': { actualWeight: 600, weightEntryMode: 'CONFIRM_REFERENCE' as const },
        'roll-2': { actualWeight: 700, weightEntryMode: 'CONFIRM_REFERENCE' as const },
        'roll-3': { actualWeight: 700, weightEntryMode: 'CONFIRM_REFERENCE' as const },
      },
    }

    const metrics = buildWorkItemMetrics(item, values)

    expect(metrics.rollActual).toBe(2000)
    expect(metrics.missingRoll).toBe(false)
    expect(metrics.missingRolls).toBe(0)
  })

  it('serializes confirmed reference intent for every selected source roll', () => {
    const detail = detailFixture()
    const values = confirmedReferenceBackRecordValues(detail)

    const dto = buildBackRecordDTO(detail, values, undefined, undefined, {
      selectedRollUuids: new Set(['roll-1', 'roll-2', 'roll-3']),
    })

    expect(dto.rolls).toHaveLength(3)
    expect(dto.rolls).toEqual(expect.arrayContaining([
      expect.objectContaining({ uuid: 'roll-1', actualWeight: 1, weightEntryMode: 'CONFIRM_REFERENCE' }),
      expect.objectContaining({ uuid: 'roll-2', actualWeight: 1, weightEntryMode: 'CONFIRM_REFERENCE' }),
      expect.objectContaining({ uuid: 'roll-3', actualWeight: 1, weightEntryMode: 'CONFIRM_REFERENCE' }),
    ]))
  })

  it('keeps estimated standard-tonnage sources pending until measured', () => {
    const item = mergeItem(1)
    const values = {
      rolls: {
        'roll-1': { actualWeight: 600, weightEntryMode: 'USER_ESTIMATE' as const },
        'roll-2': { actualWeight: 700, weightEntryMode: 'USER_ESTIMATE' as const },
        'roll-3': { actualWeight: 700, weightEntryMode: 'USER_ESTIMATE' as const },
      },
    }

    const metrics = buildWorkItemMetrics(item, values)

    expect(metrics.missingRoll).toBe(true)
    expect(metrics.missingRolls).toBe(3)
    expect(metrics.unverifiedRolls).toBe(0)
  })

  it('does not infer measurement from an estimated source when form mode is absent', () => {
    const item = mergeItem(1)
    item.rollProductions = item.rollProductions.map((production) => ({
      ...production,
      weightStatus: 'ESTIMATED',
    }))

    const metrics = buildWorkItemMetrics(item, rollValues(600, 700, 700))

    expect(metrics.missingRoll).toBe(true)
    expect(metrics.missingRolls).toBe(3)
  })

  it('marks nominal optional sources as unverified without blocking completion', () => {
    const item = mergeItem(2)
    const values = {
      rolls: {
        'roll-1': { actualWeight: 600, weightEntryMode: 'CARRY_NOMINAL' as const },
        'roll-2': { actualWeight: 700, weightEntryMode: 'CARRY_NOMINAL' as const },
        'roll-3': { actualWeight: 700, weightEntryMode: 'CARRY_NOMINAL' as const },
      },
    }

    const metrics = buildWorkItemMetrics(item, values)

    expect(metrics.missingRoll).toBe(false)
    expect(metrics.missingRolls).toBe(0)
    expect(metrics.unverifiedRolls).toBe(3)
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
