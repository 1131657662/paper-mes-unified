interface SettleNavigationState {
  from?: string
  initialOrderUuids?: unknown
}

const SETTLE_LIST_PATH = '/settle-orders'

export function settleListLocation(pathname: string, search: string) {
  return `${pathname}${search}`
}

export function settleListReturnTarget(state: unknown) {
  if (!isNavigationState(state)) return SETTLE_LIST_PATH
  if (isAllowedReturnTarget(state.from)) return state.from
  return SETTLE_LIST_PATH
}

export function initialOrderUuidsFromNavigationState(value: unknown): string[] {
  if (!isNavigationState(value) || !Array.isArray(value.initialOrderUuids)) return []
  return value.initialOrderUuids.filter((item): item is string => (
    typeof item === 'string' && /^[a-zA-Z0-9_-]{1,64}$/.test(item)
  ))
}

function isNavigationState(value: unknown): value is Required<SettleNavigationState> {
  return typeof value === 'object' && value !== null
    && 'from' in value && typeof value.from === 'string'
}

function isAllowedReturnTarget(path: string) {
  return path === SETTLE_LIST_PATH
    || path.startsWith(`${SETTLE_LIST_PATH}?`)
    || path === '/process-orders'
    || path.startsWith('/process-orders?')
    || /^\/process-orders\/[a-zA-Z0-9_-]{1,64}$/.test(path)
}
