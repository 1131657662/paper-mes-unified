import { describe, expect, it } from 'vitest'
import type { DeliveryInventoryFinish } from '../../types/deliveryInventory'
import {
  filtersForInventoryQuickFilter,
  inventoryProductLabel,
  inventoryQuickFilterFrom,
  inventoryQuickFilterValue,
  inventoryTypeFrom,
  inventoryTypeText,
  isDeliveryInventoryView,
  mergeInventorySelection,
  stockStateFrom,
} from './deliveryInventoryModel'

describe('deliveryInventoryModel', () => {
  it('仅接受后端允许的筛选值', () => {
    expect(stockStateFrom(1)).toBe(1)
    expect(stockStateFrom(3)).toBeUndefined()
    expect(inventoryTypeFrom(3)).toBe(3)
    expect(inventoryTypeFrom(4)).toBeUndefined()
    expect(isDeliveryInventoryView('finishes')).toBe(true)
    expect(isDeliveryInventoryView('unknown')).toBe(false)
  })

  it('切换库存范围时只保留一个范围参数', () => {
    const filters = { keyword: '弘丰拓', inventoryType: 1 as const }

    expect(filtersForInventoryQuickFilter(filters, 'product')).toEqual({ keyword: '弘丰拓', inventoryScope: 'product', inventoryType: undefined })
    expect(filtersForInventoryQuickFilter(filters, 'remain')).toEqual({ keyword: '弘丰拓', inventoryScope: 'remain', inventoryType: undefined })
    expect(filtersForInventoryQuickFilter(filters, 'direct')).toEqual({ keyword: '弘丰拓', inventoryScope: undefined, inventoryType: 3 })
    expect(filtersForInventoryQuickFilter(filters, 'all')).toEqual({ keyword: '弘丰拓', inventoryScope: undefined, inventoryType: undefined })
  })

  it('兼容历史类型参数并映射到新的快捷筛选', () => {
    expect(inventoryQuickFilterValue({ inventoryScope: 'product' })).toBe('product')
    expect(inventoryQuickFilterValue({ inventoryType: 2 })).toBe('remain')
    expect(inventoryQuickFilterValue({ inventoryType: 3 })).toBe('direct')
    expect(inventoryQuickFilterValue({ inventoryType: 1 })).toBe('product')
    expect(inventoryProductLabel({ inventoryType: 1 })).toBe('普通成品')
    expect(inventoryProductLabel({ inventoryScope: 'product' })).toBe('成品（含直发）')
    expect(inventoryQuickFilterFrom('remain')).toBe('remain')
    expect(inventoryQuickFilterFrom('unknown')).toBe('all')
  })

  it('按余料和原纸直发优先级输出类型文案', () => {
    expect(inventoryTypeText(1, 2)).toBe('余料')
    expect(inventoryTypeText(0, 2)).toBe('原纸直发')
    expect(inventoryTypeText(0, 1)).toBe('普通成品')
    expect(inventoryTypeText(0, 3)).toBe('整理成品')
  })

  it('跨页合并已选行并删除取消选择的行', () => {
    const first = finish('finish-1')
    const second = finish('finish-2')

    const merged = mergeInventorySelection({ 'finish-1': first }, ['finish-1', 'finish-2'], [second])
    const afterClear = mergeInventorySelection(merged, ['finish-2'], [])

    expect(Object.keys(merged)).toEqual(['finish-1', 'finish-2'])
    expect(afterClear).toEqual({ 'finish-2': second })
  })
})

function finish(finishUuid: string): DeliveryInventoryFinish {
  return { finishUuid, finishRollNo: finishUuid, remainingWeight: 10, stockState: 1 } as DeliveryInventoryFinish
}
