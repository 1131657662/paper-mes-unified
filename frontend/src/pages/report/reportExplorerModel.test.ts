import { describe, expect, it } from 'vitest'
import { drillQuery, metricField, selectedMetricItems } from './reportExplorerModel'

describe('report explorer model', () => {
  it('maps published metric codes to dimension fields', () => {
    expect(metricField('total_amount')).toBe('totalAmount')
    expect(metricField('unknown')).toBeUndefined()
  })

  it('keeps only published metrics selected by the user', () => {
    const metrics = [{ metricCode: 'order_count' }, { metricCode: 'loss_ratio_pct' }]
    expect(selectedMetricItems(metrics as never, ['loss_ratio_pct', 'missing']).map((item) => item.metricCode))
      .toEqual(['loss_ratio_pct'])
  })

  it('creates a scoped query for drilldown without changing the date range', () => {
    const query = { dateFrom: '2026-01-01', dateTo: '2026-07-21' }
    expect(drillQuery('customer', 'customer-1', query)).toEqual({ ...query, customerUuid: 'customer-1' })
    expect(drillQuery('month', '2026-01', query)).toBeUndefined()
  })
})
