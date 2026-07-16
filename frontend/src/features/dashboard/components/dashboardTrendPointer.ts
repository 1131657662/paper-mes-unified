export function axisDataIndex(event: unknown): number {
  if (!event || typeof event !== 'object') return -1
  if ('axesInfo' in event) return indexFromAxesInfo(event.axesInfo)
  if ('seriesDataIndices' in event) return indexFromSeriesData(event.seriesDataIndices)
  return -1
}

function indexFromAxesInfo(value: unknown): number {
  if (!Array.isArray(value)) return -1
  const first = value[0]
  if (!first || typeof first !== 'object') return -1
  if ('seriesDataIndices' in first) return indexFromSeriesData(first.seriesDataIndices)
  if (!('value' in first)) return -1
  const index = Number(first.value)
  return Number.isInteger(index) ? index : -1
}

function indexFromSeriesData(value: unknown): number {
  if (!Array.isArray(value)) return -1
  const first = value[0]
  if (!first || typeof first !== 'object' || !('dataIndex' in first)) return -1
  return typeof first.dataIndex === 'number' ? first.dataIndex : -1
}
