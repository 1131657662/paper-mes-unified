import { describe, expect, it } from 'vitest'
import type { RollProductionVO } from '../../types/processOrder'
import { buildProductionRouteFlow } from './productionRouteFlow'
import type { RouteNode } from './productionRouteTree'

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
