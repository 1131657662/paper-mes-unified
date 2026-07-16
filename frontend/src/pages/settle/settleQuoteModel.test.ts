import { describe, expect, it } from 'vitest'
import { applyQuoteLines } from './settleQuoteModel'

describe('applyQuoteLines', () => {
  it('replaces selected row amounts with the server quote', () => {
    const candidates = [{ orderUuid: 'o1', orderNo: 'JG1', customerUuid: 'c1',
      customerName: '客户', totalAmount: 100 }]
    const lines = [{ orderUuid: 'o1', sawAmount: 80, rewindAmount: 0, extraAmount: 20,
      amountNoTax: 100, taxAmount: 13, totalAmount: 113 }]

    const result = applyQuoteLines(candidates, lines)

    expect(result[0]?.totalAmount).toBe(113)
    expect(result[0]?.sawAmount).toBe(80)
  })
})
