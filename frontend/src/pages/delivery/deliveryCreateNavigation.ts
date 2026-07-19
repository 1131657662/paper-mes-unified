export function finishUuidsFromNavigationState(value: unknown): string[] {
  if (!value || typeof value !== 'object' || !('finishUuids' in value)) return []
  const values = Array.isArray(value.finishUuids)
    ? value.finishUuids.filter((item): item is string => typeof item === 'string')
    : []
  return [...new Set(values.filter((item) => /^[0-9a-z-]{1,64}$/i.test(item)))].slice(0, 100)
}
