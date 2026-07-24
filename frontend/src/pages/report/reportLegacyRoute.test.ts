import { describe, expect, it } from 'vitest'
import { resolveLegacyReportLocation } from './reportLegacyRoute'

describe('legacy report route', () => {
  it('maps the old topic query and keeps approved filters', () => {
    const result = resolveLegacyReportLocation(
      '?view=inventory&dateFrom=2026-01-01&dateTo=2026-07-21&customerUuid=c-1&unknown=secret',
    )

    expect(result).toEqual({
      pathname: '/reports/inventory',
      search: '?dateFrom=2026-01-01&dateTo=2026-07-21&customerUuid=c-1',
    })
  })

  it('defaults invalid topics to the overview without leaking unknown parameters', () => {
    expect(resolveLegacyReportLocation('?view=not-real&token=secret')).toEqual({
      pathname: '/reports/overview',
      search: '',
    })
  })

  it('routes alert events to the management context', () => {
    expect(resolveLegacyReportLocation('?alertEvent=event-1&view=inventory')).toEqual({
      pathname: '/reports/management/subscriptions',
      search: '?eventId=event-1',
    })
  })
})
