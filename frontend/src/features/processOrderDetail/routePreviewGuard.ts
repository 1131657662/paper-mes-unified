import type { ProcessRoutePreviewDTO } from '../../types/processOrder'

export interface RoutePreviewRequestGate {
  begin: () => number
  invalidate: () => void
  isCurrent: (requestId: number) => boolean
}

export function createRoutePreviewRequestGate(): RoutePreviewRequestGate {
  let latestRequestId = 0
  return {
    begin: () => {
      latestRequestId += 1
      return latestRequestId
    },
    invalidate: () => { latestRequestId += 1 },
    isCurrent: (requestId) => requestId === latestRequestId,
  }
}

export function routeRequestFingerprint(request: ProcessRoutePreviewDTO): string {
  return JSON.stringify(request)
}

export function isRoutePreviewCurrent(
  previewFingerprint: string | undefined,
  currentRequest: ProcessRoutePreviewDTO | undefined,
): boolean {
  return Boolean(
    previewFingerprint
    && currentRequest
    && previewFingerprint === routeRequestFingerprint(currentRequest),
  )
}
