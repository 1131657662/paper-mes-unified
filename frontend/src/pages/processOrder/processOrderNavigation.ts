export function processOrderListLocation(pathname: string, search: string): string {
  return `${pathname}${search}`
}

export function processOrderReturnTarget(state: unknown, fallback: string): string {
  if (!isListNavigationState(state)) return fallback
  return state.from
}

function isListNavigationState(value: unknown): value is { from: string } {
  return typeof value === 'object' && value !== null
    && 'from' in value
    && typeof value.from === 'string'
    && (value.from === '/process-orders' || value.from.startsWith('/process-orders?'))
}
