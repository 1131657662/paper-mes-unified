import { describe, expect, it } from 'vitest'
import type { ProcessCatalog } from '../../types/processCatalog'
import {
  customerPriceFormPatch,
  processStepInitialValues,
  processStepPayload,
  processTypeDefaults,
} from './processStepCatalogModel'

const serviceCatalog: ProcessCatalog = {
  uuid: 'strip',
  stepType: 3,
  code: 'STRIP_SORT',
  name: '剥损整理',
  category: 'SERVICE',
  pricingStrategy: 'SERVICE_QUANTITY',
  producesInventoryOutput: false,
  allowsLossRecording: true,
  allowsMainProcess: false,
  units: [
    { code: 'PIECE', name: '件', defaultUnit: true },
    { code: 'TON', name: '吨', defaultUnit: false },
  ],
  billingModes: [1, 3, 4],
}

describe('process step catalog model', () => {
  it('uses allowed current service mode and unit', () => {
    const defaults = processTypeDefaults({
      catalog: serviceCatalog,
      currentBillingMode: 3,
      currentUnit: 'TON',
      currentIsMain: 1,
    })

    expect(defaults).toEqual({ isMain: 0, billingMode: 3, billingBasis: 'TON' })
  })

  it('falls back to catalog defaults for unsupported values', () => {
    const defaults = processTypeDefaults({
      catalog: serviceCatalog,
      currentBillingMode: 2,
      currentUnit: 'BOX',
    })

    expect(defaults).toEqual({ isMain: 0, billingMode: 0, billingBasis: 'PIECE' })
  })

  it('keeps service billing fields and forces appended identity', () => {
    const payload = processStepPayload({
      values: serviceValues(),
      catalog: serviceCatalog,
      extraOnly: true,
    })

    expect(payload).toMatchObject({ isMain: 0, billingBasis: 'PIECE' })
    expect(payload.serviceQuantity).toBeUndefined()
  })

  it('removes service-only fields from production payloads', () => {
    const payload = processStepPayload({
      values: serviceValues(),
      catalog: { ...serviceCatalog, pricingStrategy: 'SAW_KNIFE' },
      extraOnly: false,
    })

    expect(payload.billingBasis).toBeUndefined()
    expect(payload.serviceQuantity).toBeUndefined()
    expect(payload.billingMode).toBeUndefined()
  })

  it('does not preselect a process type before the catalog loads', () => {
    const values = processStepInitialValues({ defaultOriginalUuid: 'roll-1' })

    expect(values).toEqual({
      originalUuid: 'roll-1',
      isMain: 0,
      billingMode: 0,
      fixedAmountScope: 'TOTAL',
    })
    expect(values.stepType).toBeUndefined()
  })

  it('applies a fixed customer default as a fixed order amount', () => {
    const result = customerPriceFormPatch({
      catalog: serviceCatalog,
      preferDefault: true,
      prices: [{ catalogUuid: 'strip', billingBasis: 'FIXED', price: 260, defaultOption: true }],
    })
    expect(result).toEqual({ billingMode: 3, billingBasis: undefined, billingAmount: 260, unitPrice: undefined })
  })

  it('uses the matching customer price when the billing basis changes', () => {
    const result = customerPriceFormPatch({
      catalog: serviceCatalog,
      billingMode: 1,
      billingBasis: 'TON',
      prices: [{ catalogUuid: 'strip', billingBasis: 'TON', price: 180, defaultOption: false }],
    })
    expect(result).toMatchObject({ billingMode: 1, billingBasis: 'TON', unitPrice: 180 })
  })
})

function serviceValues() {
  return {
    originalUuid: 'roll-1',
    stepType: 3,
    isMain: 1,
    billingMode: 1 as const,
    billingBasis: 'PIECE',
    serviceQuantity: 8,
    unitPrice: 10,
  }
}
