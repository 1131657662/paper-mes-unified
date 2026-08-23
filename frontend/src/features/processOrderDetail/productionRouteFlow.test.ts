import { describe, expect, it } from 'vitest'
import type { RollProductionVO } from '../../types/processOrder'
import { buildProductionRouteFlow } from './productionRouteFlow'
import { buildRouteTree, type RouteNode } from './productionRouteTree'
import type { StageOutputVO } from '../../types/processOrder'

describe('合并复卷加工路线', () => {
  it('为每个来源母卷生成独立节点并连接到合并产物', () => {
    const sources = [
      source('roll-1', 'MERGE-1', 2000),
      source('roll-2', 'MERGE-2', 1500),
      source('roll-3', 'MERGE-3', 1000),
    ]
    const root: RouteNode = {
      appendable: false,
      children: [],
      isTrim: false,
      key: 'finish-1',
      level: 1,
      meta: '测试纸 / 80g / 1000mm',
      processLabel: '复卷',
      statusColor: 'green',
      statusText: '最终成品',
      title: 'A001193',
      weight: 4400,
      weightDigits: 0,
      weightLabel: '实际',
    }
    const main = sources[0]
    if (!main) throw new Error('测试夹具缺少主母卷')

    const flow = buildProductionRouteFlow({
      production: main,
      roots: [root],
      sourceProductions: sources,
    })

    expect(flow.nodes.filter((node) => node.data.kind === 'source').map((node) => node.data.title))
      .toEqual(['MERGE-1', 'MERGE-2', 'MERGE-3'])
    expect(flow.edges.map((edge) => [edge.source, edge.target])).toEqual([
      ['source-roll-1', 'finish-1'],
      ['source-roll-2', 'finish-1'],
      ['source-roll-3', 'finish-1'],
    ])
  })

  it('合并阶段产物同时连接到全部上游产物', () => {
    const outputs: StageOutputVO[] = [
      stageOutput('parent-a', 1, 400),
      stageOutput('parent-b', 1, 600),
      {
        ...stageOutput('merged-finish', 2, 1000),
        inputOutputUuids: ['parent-a', 'parent-b'],
        parentOutputUuid: 'parent-a',
      },
    ]
    const production = source('roll-1', 'MERGE-1', 1000)
    const roots = buildRouteTree(outputs, [], '首道加工', production)

    const flow = buildProductionRouteFlow({ production, roots })

    expect(flow.edges.map((edge) => [edge.source, edge.target])).toEqual([
      ['source-roll-1', 'parent-a'],
      ['source-roll-1', 'parent-b'],
      ['parent-a', 'merged-finish'],
      ['parent-b', 'merged-finish'],
    ])
  })
})

function source(originalUuid: string, rollNo: string, actualWeight: number): RollProductionVO {
  return {
    actualWeight,
    gramWeight: 80,
    originalUuid,
    originalWidth: 1000,
    rollNo,
  }
}

function stageOutput(uuid: string, stageLevel: number, estimateWeight: number): StageOutputVO {
  return {
    estimateWeight,
    finishWidth: 1000,
    gramWeight: 80,
    outputStatus: 1,
    sourceStepType: stageLevel === 1 ? 1 : 2,
    stageLevel,
    uuid,
  }
}
