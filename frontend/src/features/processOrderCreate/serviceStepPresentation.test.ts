import { describe, expect, it } from 'vitest'
import { servicePricingSummary, serviceStepMatchesDraft } from './serviceStepPresentation'

describe('service step presentation', () => {
  const saved = {
    uuid: 'step-1', originalUuid: 'roll-1', stepType: 3, isMain: 0,
    stepName: '剥损整理', billingMode: 3 as const, billingAmount: 7.5,
  }

  it('treats the default fixed scope as the saved configuration', () => {
    expect(serviceStepMatchesDraft({
      stepType: 3, billingMode: 3, billingAmount: 7.5, fixedAmountScope: 'TOTAL',
    }, saved)).toBe(true)
    expect(serviceStepMatchesDraft({
      stepType: 3, billingMode: 3, billingAmount: 7.5, fixedAmountScope: 'EACH',
    }, saved)).toBe(false)
  })

  it('renders a human-readable pending pricing summary', () => {
    expect(servicePricingSummary({ stepType: 3, billingMode: 1, billingBasis: 'PIECE', unitPrice: 20 }))
      .toBe('剥损整理 · ¥20.00/件')
  })
})
