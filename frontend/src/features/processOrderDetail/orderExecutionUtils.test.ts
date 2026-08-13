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

  it('已完成历史单缺少打印确认时阻止继续流转提示', () => {
    const summary = buildExecutionSummary({
      order: { uuid: 'order-history', orderStatus: 4, printStatus: 0, printCount: 0 },
      originalRolls: [],
      rolls: [],
      steps: [],
      finishRolls: [],
      rollProductions: [],
    })

    expect(summary.statusHint).toContain('完成补打确认后才能继续流转')
    expect(summary.printableWarnings).toEqual([
      '该历史加工单没有人工确认打印记录，请完成补打确认后再出库或结算',
    ])
  })

  it('合并复卷来源母卷不要求重复保存主工序', () => {
    const mergedFinish = {
      uuid: 'finish-1',
      isSpare: 0,
      isRemain: 0,
      rollNoStatus: 1,
      finishStatus: 1,
      sources: [{ originalUuid: 'roll-1' }, { originalUuid: 'roll-2' }],
    }
    const summary = buildExecutionSummary({
      order: { uuid: 'order-1', orderStatus: 1 },
      originalRolls: [],
      rolls: [],
      steps: [],
      finishRolls: [{ uuid: 'finish-1', isSpare: 0, isRemain: 0, rollNoStatus: 1 }],
      rollProductions: [
        {
          originalUuid: 'roll-1', processMode: 1, mainStepType: 2,
          steps: [{ uuid: 'step-1', originalUuid: 'roll-1', stepType: 2, isMain: 1 }],
          rewindParams: [{ paramMode: 5 }], finishes: [mergedFinish],
        },
        {
          originalUuid: 'roll-2', processMode: 1, mainStepType: 2,
          steps: [], finishes: [mergedFinish],
        },
      ],
    })

    expect(summary.printableWarnings).toEqual([])
  })

  it('普通多来源成品仍提示缺少主工序', () => {
    const summary = buildExecutionSummary({
      order: { uuid: 'order-1', orderStatus: 1 },
      originalRolls: [],
      rolls: [],
      steps: [],
      finishRolls: [{ uuid: 'finish-1', isSpare: 0, isRemain: 0, rollNoStatus: 1 }],
      rollProductions: [{
        originalUuid: 'roll-2', processMode: 1, mainStepType: 2, steps: [],
        finishes: [{
          uuid: 'finish-1', isSpare: 0, isRemain: 0, rollNoStatus: 1,
          sources: [{ originalUuid: 'roll-1' }, { originalUuid: 'roll-2' }],
        }],
      }],
    })

    expect(summary.printableWarnings).toContain('1 卷缺少主工序，打印下发会被后端拦截')
  })

  it('重复来源关系不视为多母卷合并复卷', () => {
    const summary = buildExecutionSummary({
      order: { uuid: 'order-1', orderStatus: 1 },
      originalRolls: [], rolls: [], steps: [],
      finishRolls: [{ uuid: 'finish-1', isSpare: 0, isRemain: 0, rollNoStatus: 1 }],
      rollProductions: [{
        originalUuid: 'roll-2', processMode: 1, mainStepType: 2,
        steps: [{ uuid: 'step-1', originalUuid: 'roll-2', stepType: 2, isMain: 1 }],
        rewindParams: [{ paramMode: 5 }],
        finishes: [{
          uuid: 'finish-1', isSpare: 0, isRemain: 0, rollNoStatus: 1,
          sources: [{ originalUuid: 'roll-1' }, { originalUuid: 'roll-1' }],
        }],
      }, {
        originalUuid: 'roll-1', processMode: 1, mainStepType: 2, steps: [], finishes: [],
      }],
    })

    expect(summary.printableWarnings).toContain('1 卷缺少主工序，打印下发会被后端拦截')
  })
})
