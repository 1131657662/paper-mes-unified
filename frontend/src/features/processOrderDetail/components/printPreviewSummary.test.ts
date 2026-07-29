import { describe, expect, it } from 'vitest'
import type { ProcessOrderDetailVO, RollProductionVO } from '../../../types/processOrder'
import { buildPrintSummary } from './printPreviewSummary'

describe('加工单打印汇总', () => {
  it('纯复卷单只显示有现场价值的项目', () => {
    const summary = buildPrintSummary(detail([production('roll-1', 2), production('roll-2', 2)]))

    expect(summary.map((item) => item.label)).toEqual(['原卷', '最终成品', '复卷'])
    expect(summary[2]?.value).toBe('2 卷 / 5 t')
  })

  it('混合单动态显示非零工艺数量', () => {
    const saw = production('saw-roll', 1)
    saw.steps = [{ uuid: 'saw', originalUuid: 'saw-roll', stepType: 1, knifeCount: 3 }]
    const direct = production('direct-roll', 2)
    direct.processMode = 3

    const summary = buildPrintSummary(detail([saw, production('rewind-roll', 2), direct]))

    expect(summary).toContainEqual({ label: '锯纸', value: '1 卷 / 3 刀' })
    expect(summary).toContainEqual({ label: '复卷', value: '1 卷 / 2.5 t' })
    expect(summary).toContainEqual({ label: '直发', value: '1 卷' })
    expect(summary.some((item) => item.label === '工序数')).toBe(false)
    expect(summary.some((item) => item.label === '订单标记')).toBe(false)
  })
})

function detail(productions: RollProductionVO[]): ProcessOrderDetailVO {
  const steps = productions.flatMap((item) => item.steps ?? [])
  return {
    order: { uuid: 'order-1' },
    originalRolls: [],
    rolls: [],
    finishRolls: [],
    steps,
    rollProductions: productions,
  }
}

function production(uuid: string, stepType: number): RollProductionVO {
  return {
    originalUuid: uuid,
    paperName: '牛卡',
    gramWeight: 125,
    originalWidth: 1250,
    rollWeight: 2500,
    pieceNum: 1,
    processMode: 1,
    mainStepType: stepType,
    steps: stepType === 2
      ? [{ uuid: `${uuid}-step`, originalUuid: uuid, stepType: 2, processWeight: 2500 }]
      : [],
    finishes: [],
  }
}
