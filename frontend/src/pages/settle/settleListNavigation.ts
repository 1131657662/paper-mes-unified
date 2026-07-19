interface SettleNavigationState {
  from?: string
}

const SETTLE_LIST_PATH = '/settle-orders'

export function settleListLocation(pathname: string, search: string) {
  return `${pathname}${search}`
}

export function settleListReturnTarget(state: unknown) {
  if (!isNavigationState(state)) return SETTLE_LIST_PATH
  if (state.from === SETTLE_LIST_PATH || state.from.startsWith(`${SETTLE_LIST_PATH}?`)) return state.from
  return SETTLE_LIST_PATH
}

function isNavigationState(value: unknown): value is Required<SettleNavigationState> {
  return typeof value === 'object' && value !== null
    && 'from' in value && typeof value.from === 'string'
}
