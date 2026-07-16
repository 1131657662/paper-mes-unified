import { describe, expect, it } from 'vitest'
import type { FinishedProductRow } from './finishedProductRows'
import { buildCustomerFinishedProductRows } from './customerFinishedProductRows'

describe('客户成品规格汇总', () => {
  it('忽略直径和纸芯并合并相同门幅的成品', () => {
    const rows = [finishedRow('finish-1', 1000, 3), finishedRow('finish-2', 1200, 6)]

    const result = buildCustomerFinishedProductRows(rows)

    expect(result).toHaveLength(1)
    expect(result[0]).toMatchObject({ count: 2, weight: 1900, width: 900 })
  })

  it('按品名克重规格和件重排列规格汇总', () => {
    const rows = [
      specificationRow({ uuid: 'finish-255', gramWeight: 255, width: 1036, weight: 799 }),
      specificationRow({ uuid: 'finish-240', gramWeight: 240, width: 1166, weight: 836 }),
      specificationRow({ uuid: 'finish-220-wide', gramWeight: 220, width: 992, weight: 531 }),
      specificationRow({ uuid: 'finish-232', gramWeight: 232, width: 1045, weight: 654 }),
      specificationRow({ uuid: 'finish-220-narrow', gramWeight: 220, width: 878, weight: 471 }),
    ]

    const result = buildCustomerFinishedProductRows(rows)

    expect(result.map((row) => [row.gramWeight, row.width])).toEqual([
      [220, 878],
      [220, 992],
      [232, 1045],
      [240, 1166],
      [255, 1036],
    ])
  })
})

function finishedRow(uuid: string, diameter: number, coreDiameter: number): FinishedProductRow {
  return {
    key: uuid,
    sources: [],
    finish: {
      uuid,
      paperName: '白卡',
      gramWeight: 300,
      finishWidth: 900,
      finishDiameter: diameter,
      finishCoreDiameter: coreDiameter,
      actualWeight: 950,
    },
  }
}

function specificationRow(options: {
  gramWeight: number
  uuid: string
  weight: number
  width: number
}): FinishedProductRow {
  return {
    key: options.uuid,
    sources: [],
    finish: {
      uuid: options.uuid,
      paperName: '瑞典恩索涂布牛卡纸',
      gramWeight: options.gramWeight,
      finishWidth: options.width,
      actualWeight: options.weight,
    },
  }
}
