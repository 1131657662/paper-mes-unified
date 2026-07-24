export function finishUuidsFromNavigationState(value: unknown): string[] {
  if (!value || typeof value !== 'object' || !('finishUuids' in value)) return []
  const values = Array.isArray(value.finishUuids)
    ? value.finishUuids.filter((item): item is string => typeof item === 'string')
    : []
  return [...new Set(values.filter((item) => /^[0-9a-z-]{1,64}$/i.test(item)))].slice(0, 100)
}

export function deliveryCreateReturnTarget(value: unknown): string {
  if (!isNavigationState(value) || typeof value.from !== 'string') return '/delivery-orders'
  if (value.from === '/process-orders' || value.from.startsWith('/process-orders?')) return value.from
  if (value.from === '/delivery-orders' || value.from.startsWith('/delivery-orders?')) return value.from
  if (/^\/process-orders\/[a-zA-Z0-9_-]{1,64}$/.test(value.from)) return value.from
  return '/delivery-orders'
}

function isNavigationState(value: unknown): value is { finishUuids?: unknown; from?: unknown } {
  return typeof value === 'object' && value !== null
}
