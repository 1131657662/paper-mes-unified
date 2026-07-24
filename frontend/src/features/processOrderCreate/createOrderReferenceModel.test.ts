import { describe, expect, it } from 'vitest'
import type { Customer } from '../../types/customer'
import {
  createDefaultPlanOptions,
  toCustomerOptions,
  toReferenceOptions,
} from './createOrderReferenceModel'

function customer(overrides: Partial<Customer> = {}): Customer {
  return {
    uuid: 'customer-1',
    customerName: '测试客户',
    ...overrides,
  }
}

describe('createOrderReferenceModel', () => {
  it('maps customer pricing defaults without dropping settlement fields', () => {
    const options = toCustomerOptions([customer({
      defaultInvoice: 1,
      rewindPrice: 210,
      sawPrice: 1.8,
      settleDay: 30,
      settleType: 2,
      taxRate: 13,
    })])

    expect(options[0]).toMatchObject({
      label: '测试客户',
      value: 'customer-1',
      defaultInvoice: 1,
      rewindPrice: 210,
      sawPrice: 1.8,
      settleDay: 30,
      settleType: 2,
      taxRate: 13,
    })
  })

  it('builds plan defaults from the selected customer and system spare count', () => {
    const options = createDefaultPlanOptions(customer({ rewindPrice: 200, sawPrice: 1.5 }), 2)

    expect(options).toEqual({ spareCount: 2, rewindPrice: 200, sawPrice: 1.5 })
  })

  it('maps generic reference records using the requested label field', () => {
    const options = toReferenceOptions([
      { uuid: 'warehouse-1', warehouseName: '一号仓' },
    ], 'warehouseName')

    expect(options).toEqual([{ label: '一号仓', value: 'warehouse-1' }])
  })
})
