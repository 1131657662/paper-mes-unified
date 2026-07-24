import { describe, expect, it } from 'vitest'
import type { ProcessRoutePreviewDTO } from '../../types/processOrder'
import { isRoutePreviewCurrent, routeRequestFingerprint } from './routePreviewGuard'

const request = (unitPrice: number): ProcessRoutePreviewDTO => ({
  originalUuid: 'roll-1',
  stages: [{ stageLevel: 1, stepType: 1, unitPrice, outputs: [] }],
})

describe('route preview guard', () => {
  it('allows saving when the preview matches the current route request', () => {
    const current = request(12)

    expect(isRoutePreviewCurrent(routeRequestFingerprint(current), current)).toBe(true)
  })

  it('blocks saving after the route form changes', () => {
    expect(isRoutePreviewCurrent(routeRequestFingerprint(request(12)), request(13))).toBe(false)
  })

  it('blocks saving before a preview exists', () => {
    expect(isRoutePreviewCurrent(undefined, request(12))).toBe(false)
  })
})
