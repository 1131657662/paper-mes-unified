import { describe, expect, it } from 'vitest'
import { buildDenseBodies } from './printDenseTableModel'
import type { PrintRollBlock } from './printPreviewTypes'

describe('加工单紧凑表格分页分组', () => {
  it('每个母卷形成独立表体并保留工艺标题', () => {
    const bodies = buildDenseBodies([block('母卷 1'), block('母卷 2')])

    expect(bodies.map((body) => body.kind)).toEqual(['process', 'roll', 'roll'])
    expect(bodies[1]).toMatchObject({ kind: 'roll' })
    expect(bodies[2]).toMatchObject({ kind: 'roll' })
    if (bodies[1]?.kind === 'roll') expect(bodies[1].rows[0]?.blockRowSpan).toBe(2)
    if (bodies[2]?.kind === 'roll') expect(bodies[2].rows[0]?.blockRowSpan).toBe(2)
  })
})

function block(title: string): PrintRollBlock {
  return {
    key: title,
    title,
    sourceItems: [],
    routeStages: [{
      key: `${title}-stage`,
      stepType: 2,
      title: '第1道 复卷',
      source: '原卷',
      metric: '复卷 1 t',
      requirement: '加工门幅 1000 mm，产出 2 件。',
      outputs: [output(`${title}-1`), output(`${title}-2`)],
    }],
  }
}

function output(key: string) {
  return {
    key,
    name: key,
    spec: '牛卡 / 125 g / 1000 mm',
    weight: '500 kg',
    status: 'final' as const,
  }
}
