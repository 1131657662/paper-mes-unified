import type { ProcessRoutePreviewDTO } from '../../types/processOrder'

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
