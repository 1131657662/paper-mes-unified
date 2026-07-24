import { describe, expect, it } from 'vitest'
import { needsOperationalPaperCandidates, reportOperationalQuery } from './reportOperationalQuery'

const source = {
  customerUuid: 'customer', dateFrom: '2026-01-01', dateTo: '2026-07-21',
  paperName: '牛卡', machineUuid: 'machine', settleType: 2, isInvoice: 1,
  processMode: 2, orderStatus: 4,
}

describe('reportOperationalQuery', () => {
  it('keeps only financial filters for settlement', () => {
    expect(reportOperationalQuery('settlement', source)).toEqual({
      customerUuid: 'customer', dateFrom: '2026-01-01', dateTo: '2026-07-21',
      metricReleaseUuid: undefined, settleType: 2, isInvoice: 1,
    })
  })

  it('keeps stock filters for inventory', () => {
    expect(reportOperationalQuery('inventory', source)).toEqual({
      customerUuid: 'customer', dateFrom: '2026-01-01', dateTo: '2026-07-21',
      metricReleaseUuid: undefined, paperName: '牛卡',
    })
  })

  it('removes unrelated filters for delivery', () => {
    expect(reportOperationalQuery('delivery', source)).toEqual({
      customerUuid: 'customer', dateFrom: '2026-01-01', dateTo: '2026-07-21',
      metricReleaseUuid: undefined,
    })
  })

  it('loads paper candidates only for the inventory topic', () => {
    expect(needsOperationalPaperCandidates('inventory')).toBe(true)
    expect(needsOperationalPaperCandidates('settlement')).toBe(false)
    expect(needsOperationalPaperCandidates('collection')).toBe(false)
    expect(needsOperationalPaperCandidates('delivery')).toBe(false)
  })
})
