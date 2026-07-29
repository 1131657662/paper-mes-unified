import { describe, expect, it } from 'vitest'
import { buildProcessingFlow, groupFinishes } from './detailHelpers'

describe('母卷成品规格分组排序', () => {
  it('按成品门幅从小到大排列', () => {
    const groups = groupFinishes([
      { uuid: 'finish-wide', finishWidth: 992, estimateWeight: 500 },
      { uuid: 'finish-narrow', finishWidth: 878, estimateWeight: 400 },
      { uuid: 'finish-middle', finishWidth: 900, estimateWeight: 450 },
    ])

    expect(groups.map((group) => group.width)).toEqual([878, 900, 992])
  })
})

describe('加工方案门幅差额展示', () => {
  it('打印与详情流程展示已保存的计划损耗策略', () => {
    const flow = buildProcessingFlow({
      originalUuid: 'roll-1',
      processMode: 1,
      mainStepType: 2,
      originalWidth: 1500,
      rewindParams: [{ paramMode: 1 }],
      finishes: [{ uuid: 'finish-1', finishWidth: 1480, estimateWeight: 789.333 }],
      steps: [{
        uuid: 'step-1',
        isMain: 1,
        widthDifferencePolicy: 'LOSS',
        plannedLossWidth: 20,
        plannedLossWeight: 10.667,
      }],
    })

    expect(flow.flatMap((step) => step.details))
      .toContain('门幅差额：计划损耗 20 mm / 10.667 kg')
  })
})
