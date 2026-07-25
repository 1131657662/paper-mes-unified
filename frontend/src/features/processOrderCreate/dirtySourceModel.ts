export type CreateOrderDirtySource = 'draft' | 'service'

export function addDirtySource(
  sources: ReadonlySet<CreateOrderDirtySource>,
  source: CreateOrderDirtySource,
): Set<CreateOrderDirtySource> {
  return new Set([...sources, source])
}

export function removeDirtySource(
  sources: ReadonlySet<CreateOrderDirtySource>,
  source: CreateOrderDirtySource,
): Set<CreateOrderDirtySource> {
  const next = new Set(sources)
  next.delete(source)
  return next
}
