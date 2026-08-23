export interface SourceConsumptionRatioInput {
  originalUuid?: string
  consumeRatio?: number
}

/** Resolves explicit and legacy empty source ratios without consuming a source twice. */
export function effectiveSourceConsumptionRatios<T extends SourceConsumptionRatioInput>(
  sources: T[],
): Map<T, number> {
  const explicit = new Map<string, number>()
  for (const source of sources) {
    const ratio = Number(source.consumeRatio ?? 0)
    if (!Number.isFinite(ratio) || ratio < 0 || ratio > 100) {
      throw new Error('来源消耗比例必须在0到100%之间')
    }
    if (ratio > 0 && source.originalUuid) {
      explicit.set(source.originalUuid, (explicit.get(source.originalUuid) ?? 0) + ratio)
    }
  }
  const consumed = new Map<string, number>()
  const legacyAssigned = new Set<string>()
  const result = new Map<T, number>()
  for (const source of sources) {
    const sourceKey = source.originalUuid ?? ''
    const raw = Number(source.consumeRatio ?? 0)
    const requested = raw > 0
      ? raw
      : legacyAssigned.has(sourceKey) ? 0 : 100 - (explicit.get(sourceKey) ?? 0)
    const used = consumed.get(sourceKey) ?? 0
    const applied = Math.min(Math.max(0, requested), Math.max(0, 100 - used))
    result.set(source, applied)
    consumed.set(sourceKey, used + applied)
    if (raw <= 0) legacyAssigned.add(sourceKey)
  }
  return result
}

export function validateExplicitSourceConsumptionRatios(
  sources: SourceConsumptionRatioInput[],
): void {
  const totals = new Map<string, number>()
  for (const source of sources) {
    const ratio = Number(source.consumeRatio ?? 0)
    if (ratio > 0 && source.originalUuid) {
      totals.set(source.originalUuid, (totals.get(source.originalUuid) ?? 0) + ratio)
    }
  }
  if (Array.from(totals.values()).some((ratio) => ratio > 100)) {
    throw new Error('来源消耗比例合计不能超过100%')
  }
}
