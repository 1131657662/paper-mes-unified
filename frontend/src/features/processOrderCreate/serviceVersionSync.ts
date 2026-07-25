import type { ProcessOrderDetailVO } from '../../types/processOrder'

export function freshDraftVersion(
  detail: ProcessOrderDetailVO | undefined,
  currentVersion: number,
): number | undefined {
  const version = detail?.order.version
  if (version == null || !Number.isInteger(version) || version <= currentVersion) return undefined
  return version
}
