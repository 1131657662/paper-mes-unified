import { describe, expect, it } from 'vitest'
import type { ProcessOrderDetailVO, RollProductionVO } from '../../../types/processOrder'
import { buildPrintRollBlocks } from './printPreviewModel'

describe('完工打印模型', () => {
  it('使用实际母卷规格和成品重量生成完工内容', () => {
    const blocks = buildPrintRollBlocks(detail())

    expect(blocks[0]?.sourceItems).toContainEqual({ label: '克重/门幅', value: '82 g（实） / 1180 mm（实）' })
    expect(blocks[0]?.sourceItems).toContainEqual({ label: '实重', value: '980 kg' })
    expect(blocks[0]?.routeStages[0]?.outputs[0]?.actualWeight).toBe('480 kg')
  })

  it('下发内容没有实测值时保留计划规格和标重', () => {
    const issued = production()
    issued.actualGramWeight = undefined
    issued.actualWidth = undefined
    issued.actualWeight = undefined

    const blocks = buildPrintRollBlocks({ ...detail(), rollProductions: [issued] })

    expect(blocks[0]?.sourceItems).toContainEqual({ label: '克重/门幅', value: '80 g / 1200 mm' })
    expect(blocks[0]?.sourceItems).toContainEqual({ label: '标重', value: '1000 kg' })
  })
})

function detail(): ProcessOrderDetailVO {
  return {
    order: { uuid: 'order-1' },
    originalRolls: [],
    rolls: [],
    finishRolls: [{ uuid: 'finish-1', actualWeight: 480 }],
    steps: [],
    rollProductions: [production()],
  }
}

function production(): RollProductionVO {
  return {
    originalUuid: 'roll-1',
    paperName: '测试纸',
    gramWeight: 80,
    actualGramWeight: 82,
    originalWidth: 1200,
    actualWidth: 1180,
    rollWeight: 1000,
    actualWeight: 980,
    pieceNum: 1,
    mainStepType: 2,
    steps: [],
    stageOutputs: [],
    rewindParams: [],
    finishes: [{
      uuid: 'finish-1',
      finishRollNo: 'A001',
      paperName: '测试纸',
      gramWeight: 82,
      finishWidth: 590,
      estimateWeight: 490,
      actualWeight: 480,
      sources: [],
    }],
  }
}
