import { describe, expect, it } from 'vitest'
import type { FinishRoll } from '../../../types/processOrder'
import { applyPrintAnnotations } from './printPreviewAnnotations'
import type { PrintRollBlock, PrintRouteOutput } from './printPreviewTypes'

describe('加工单标注要求归并', () => {
  it('按最终有效标注值上提整单克重', () => {
    const finishes = [
      finish('f1', { gramWeight: 245, customerGramWeight: 250 }),
      finish('f2', { gramWeight: 250 }),
    ]

    const model = applyPrintAnnotations(finishes, [block('b1', ['f1']), block('b2', ['f2'])])

    expect(model.orderAnnotations).toEqual([{ field: 'gramWeight', value: '250' }])
    expect(model.blocks.every((item) => item.annotations == null)).toBe(true)
  })

  it('整单不一致时将一致字段上提到各母卷', () => {
    const finishes = [
      finish('f1', { customerGramWeight: 250 }),
      finish('f2', { customerGramWeight: 250 }),
      finish('f3', { customerGramWeight: 255 }),
    ]

    const model = applyPrintAnnotations(finishes, [block('b1', ['f1', 'f2']), block('b2', ['f3'])])

    expect(model.orderAnnotations).toEqual([])
    expect(model.blocks[0]?.annotations).toEqual([{ field: 'gramWeight', value: '250' }])
    expect(model.blocks[1]?.annotations).toEqual([{ field: 'gramWeight', value: '255' }])
  })

  it('同一母卷内标注值不一致时保留在成品行', () => {
    const finishes = [
      finish('f1', { customerGramWeight: 250 }),
      finish('f2', { customerGramWeight: 255 }),
    ]

    const model = applyPrintAnnotations(finishes, [block('b1', ['f1', 'f2'])])
    const outputs = model.blocks[0]?.routeStages[0]?.outputs

    expect(outputs?.[0]?.annotations).toEqual([{ field: 'gramWeight', value: '250' }])
    expect(outputs?.[1]?.annotations).toEqual([{ field: 'gramWeight', value: '255' }])
  })

  it('各字段独立选择显示层级', () => {
    const finishes = [
      finish('f1', { gramWeight: 245, customerGramWeight: 250, customerPaperName: '食品卡' }),
      finish('f2', { gramWeight: 250 }),
    ]

    const model = applyPrintAnnotations(finishes, [block('b1', ['f1', 'f2'])])
    const outputs = model.blocks[0]?.routeStages[0]?.outputs

    expect(model.orderAnnotations).toEqual([{ field: 'gramWeight', value: '250' }])
    expect(outputs?.[0]?.annotations).toEqual([{ field: 'paperName', value: '食品卡' }])
    expect(outputs?.[1]?.annotations).toBeUndefined()
  })

  it('成品关联缺失时不做上提', () => {
    const finishes = [finish('f1', { customerFinishWidth: 1180 })]

    const model = applyPrintAnnotations(finishes, [block('b1', ['f1', 'missing'])])
    const outputs = model.blocks[0]?.routeStages[0]?.outputs

    expect(model.orderAnnotations).toEqual([])
    expect(model.blocks[0]?.annotations).toBeUndefined()
    expect(outputs?.[0]?.annotations).toEqual([{ field: 'finishWidth', value: '1180' }])
  })

  it('忽略空值相同值和备用成品', () => {
    const finishes = [
      finish('f1', { customerPaperName: ' ', customerGramWeight: 245 }),
      finish('f2', { isSpare: 1, customerGramWeight: 250 }),
    ]

    const model = applyPrintAnnotations(finishes, [block('b1', ['f1', 'f2'])])

    expect(model.orderAnnotations).toEqual([])
    expect(model.blocks[0]?.annotations).toBeUndefined()
    expect(model.blocks[0]?.routeStages[0]?.outputs.every((item) => item.annotations == null)).toBe(true)
  })
})

function finish(uuid: string, values: Partial<FinishRoll> = {}): FinishRoll {
  return {
    uuid,
    paperName: '涂布牛卡',
    gramWeight: 245,
    finishWidth: 1600,
    ...values,
  }
}

function block(key: string, finishUuids: string[]): PrintRollBlock {
  return {
    key,
    title: key,
    sourceItems: [],
    routeStages: [{
      key: `${key}-stage`,
      title: '锯纸',
      source: '原卷',
      metric: '-',
      requirement: '-',
      outputs: finishUuids.map(output),
    }],
  }
}

function output(finishRollUuid: string): PrintRouteOutput {
  return {
    key: `output-${finishRollUuid}`,
    finishRollUuid,
    name: finishRollUuid,
    spec: '-',
    weight: '-',
    status: 'final',
  }
}
