import { describe, expect, it } from 'vitest'
import { createPayload, resolveRouteTemplate } from './rum'

describe('first-party RUM payload', () => {
  it('maps dynamic paths to route templates without query data', () => {
    expect(resolveRouteTemplate('/customers/secret-uuid')).toBe(
      '/customers/:uuid',
    )
    expect(resolveRouteTemplate('/customers/secret-uuid?name=restricted')).toBe(
      '/customers/:uuid',
    )
  })

  it('maps the public login route without retaining redirect data', () => {
    expect(resolveRouteTemplate('/login?from=/process-orders')).toBe('/login')
  })

  it('contains only the approved anonymous fields', () => {
    const payload = createPayload(
      { name: 'LCP', value: 842.5, rating: 'good' },
      {
        route: '/dashboard',
        browser: 'chrome',
        browserVersion: '136',
        deviceTier: 'mid',
        networkType: '4g',
      },
    )

    expect(Object.keys(payload).sort()).toEqual(
      [
        'browser',
        'browserVersion',
        'deviceTier',
        'name',
        'networkType',
        'rating',
        'route',
        'value',
      ].sort(),
    )
    expect(JSON.stringify(payload)).not.toContain('secret-uuid')
  })
})
