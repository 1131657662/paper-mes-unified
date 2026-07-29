import { describe, expect, it } from 'vitest'
import type { ProcessRoutePreviewDTO } from '../../types/processOrder'
import {
  createRoutePreviewRequestGate,
  isRoutePreviewCurrent,
  routeRequestFingerprint,
} from './routePreviewGuard'

const request = (unitPrice: number): ProcessRoutePreviewDTO => ({
  originalUuid: 'roll-1',
  stages: [{ stageLevel: 1, stepType: 1, unitPrice, outputs: [] }],
})

describe('route preview guard', () => {
  it('accepts only the latest response when requests resolve out of order', () => {
    const gate = createRoutePreviewRequestGate()
    const firstRequest = gate.begin()
    const secondRequest = gate.begin()

    expect(gate.isCurrent(firstRequest)).toBe(false)
    expect(gate.isCurrent(secondRequest)).toBe(true)
  })

  it('rejects a pending response after the route form changes', () => {
    const gate = createRoutePreviewRequestGate()
    const requestId = gate.begin()

    gate.invalidate()

    expect(gate.isCurrent(requestId)).toBe(false)
  })

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
