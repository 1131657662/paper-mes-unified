export type DeliveryFinishScope = 'product' | 'remain'

interface FinishKindLike {
  isRemain?: number
}

export function filterFinishesByScope<T extends FinishKindLike>(
  finishes: T[],
  scope: DeliveryFinishScope,
): T[] {
  return finishes.filter((item) => (scope === 'remain' ? item.isRemain === 1 : item.isRemain !== 1))
}

export function finishScopeName(scope: DeliveryFinishScope): string {
  return scope === 'remain' ? '余料' : '成品'
}
