import { describe, expect, it } from 'vitest'
import { toggleCustomerPrice, updateCustomerPrice } from './customerProcessPriceModel'

describe('customer process price model', () => {
  it('makes the first enabled option the default', () => {
    const result = toggleCustomerPrice({ values: [], catalogUuid: 'strip', basis: 'PIECE', checked: true })
    expect(result[0]?.isDefault).toBe(1)
  })

  it('keeps one default while enabling alternatives', () => {
    const first = toggleCustomerPrice({ values: [], catalogUuid: 'strip', basis: 'PIECE', checked: true })
    const result = toggleCustomerPrice({ values: first, catalogUuid: 'strip', basis: 'TON', checked: true })
    expect(result.map((item) => item.isDefault)).toEqual([1, 0])
  })

  it('moves the default to the selected option', () => {
    const values = [
      { catalogUuid: 'strip', billingBasis: 'PIECE' as const, price: 5, isDefault: 1 },
      { catalogUuid: 'strip', billingBasis: 'TON' as const, price: 100, isDefault: 0 },
    ]
    const result = updateCustomerPrice({ values, catalogUuid: 'strip', basis: 'TON', changes: { isDefault: 1 } })
    expect(result.map((item) => item.isDefault)).toEqual([0, 1])
  })

  it('promotes a remaining option when removing the default', () => {
    const values = [
      { catalogUuid: 'strip', billingBasis: 'PIECE' as const, price: 5, isDefault: 1 },
      { catalogUuid: 'strip', billingBasis: 'FIXED' as const, price: 80, isDefault: 0 },
    ]
    const result = toggleCustomerPrice({ values, catalogUuid: 'strip', basis: 'PIECE', checked: false })
    expect(result).toEqual([{ catalogUuid: 'strip', billingBasis: 'FIXED', price: 80, isDefault: 1 }])
  })
})
