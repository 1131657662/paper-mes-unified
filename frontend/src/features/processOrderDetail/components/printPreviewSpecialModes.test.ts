import { describe, expect, it } from 'vitest'
import type {
  FinishRoll,
  ProcessOrderDetailVO,
  RollProductionVO,
} from '../../../types/processOrder'
import { buildPrintSheetModel } from './printPreviewModel'

describe('加工单特殊生产模式打印', () => {
  it('合并复卷完整打印全部来源母卷', () => {
    const first = production('roll-1', 'MW-001', 2400)
    const second = production('roll-2', 'MW-002', 2600)
    const mergedFinish = finishProduction('finish-1', 'A001', ['roll-1', 'roll-2'])
    first.rewindParams = [{ paramMode: 5 }]
    first.finishes = [mergedFinish]
    second.finishes = [mergedFinish]

    const model = buildPrintSheetModel(detail([first, second]))

    expect(model.blocks).toHaveLength(1)
    expect(sourceValue(model.blocks[0], '卷号/编号')).toContain('MW-001')
    expect(sourceValue(model.blocks[0], '卷号/编号')).toContain('MW-002')
    expect(sourceValue(model.blocks[0], '标重')).toContain('2400 kg')
    expect(sourceValue(model.blocks[0], '标重')).toContain('2600 kg')
    expect(model.blocks[0]?.routeStages[0]?.outputs).toHaveLength(1)
  })

  it('合并复卷分别展示参考重量和未知重量', () => {
    const estimated = production('roll-1', 'MW-EST', 800)
    estimated.weightStatus = 'ESTIMATED'
    const unknown = production('roll-2', 'MW-UNKNOWN', 0)
    unknown.weightStatus = 'UNKNOWN'
    const mergedFinish = finishProduction('finish-1', 'A001', ['roll-1', 'roll-2'])
    estimated.rewindParams = [{ paramMode: 5 }]
    estimated.finishes = [mergedFinish]
    unknown.finishes = [mergedFinish]

    const model = buildPrintSheetModel(detail([estimated, unknown]))
    const weightText = sourceValue(model.blocks[0], '标重')

    expect(weightText).toContain('MW-EST：800 kg')
    expect(weightText).toContain('MW-UNKNOWN：待称重')
    expect(weightText).not.toContain('MW-UNKNOWN：0 kg')
  })

  it('不加工直发打印正确工艺和交付产物', () => {
    const direct = production('roll-1', 'MW-001', 2400)
    direct.processMode = 3
    direct.finishes = []

    const model = buildPrintSheetModel(detail([direct]))
    const stage = model.blocks[0]?.routeStages[0]

    expect(stage?.title).toBe('不加工直发')
    expect(stage?.requirement).toContain('无需加工')
    expect(stage?.outputs[0]).toMatchObject({ name: 'MW-001', status: 'final' })
  })

  it('直发成品标签要求归并到母卷', () => {
    const direct = production('roll-1', 'MW-001', 2400)
    direct.processMode = 3
    direct.finishes = []
    const regular = production('roll-2', 'MW-002', 2400)
    regular.finishes = [finishProduction('regular-finish', 'A002', ['roll-2'])]
    const directFinish = finish('direct-finish', {
      finishRollNo: 'MW-001',
      sourceType: 2,
      gramWeight: 125,
      customerGramWeight: 135,
    })
    const regularFinish = finish('regular-finish', { finishRollNo: 'A002' })

    const model = buildPrintSheetModel(detail([direct, regular], [directFinish, regularFinish]))
    const directBlock = model.blocks.find((block) => block.title === 'MW-001')

    expect(directBlock?.annotations).toEqual([{ field: 'gramWeight', value: '135' }])
  })
})

function detail(
  productions: RollProductionVO[],
  finishes: FinishRoll[] = [finish('finish-1')],
): ProcessOrderDetailVO {
  return {
    order: { uuid: 'order-1' },
    originalRolls: [],
    rolls: [],
    finishRolls: finishes,
    steps: [],
    rollProductions: productions,
  }
}

function production(uuid: string, rollNo: string, rollWeight: number): RollProductionVO {
  return {
    originalUuid: uuid,
    rollNo,
    paperName: '牛卡',
    gramWeight: 125,
    originalWidth: 1250,
    rollWeight,
    pieceNum: 1,
    processMode: 1,
    mainStepType: 2,
    steps: [],
    stageOutputs: [],
    rewindParams: [],
    finishes: [],
  }
}

function finish(uuid: string, values: Partial<FinishRoll> = {}): FinishRoll {
  return {
    uuid,
    finishRollNo: 'A001',
    paperName: '牛卡',
    gramWeight: 125,
    finishWidth: 850,
    ...values,
  }
}

function finishProduction(uuid: string, rollNo: string, sourceUuids: string[]) {
  return {
    uuid,
    finishRollNo: rollNo,
    paperName: '牛卡',
    gramWeight: 125,
    finishWidth: 850,
    estimateWeight: 2200,
    sources: sourceUuids.map((originalUuid) => ({ originalUuid })),
  }
}

function sourceValue(
  block: ReturnType<typeof buildPrintSheetModel>['blocks'][number] | undefined,
  label: string,
) {
  return block?.sourceItems.find((item) => item.label === label)?.value ?? ''
}
