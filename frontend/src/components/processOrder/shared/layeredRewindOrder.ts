import type { LayeredRewindItem, LayeredRewindLayer } from './layeredRewindView'

export interface LayeredRewindSort {
  displayOrder: number
  layerIndex: number
}

export function layerItemSortMap<T>(
  layers: Array<LayeredRewindLayer<T>>,
): Map<string, LayeredRewindSort> {
  const result = new Map<string, LayeredRewindSort>()
  let displayOrder = 0
  for (const layer of layers) {
    for (const row of layerDisplayItems(layer)) {
      result.set(row.key, { displayOrder, layerIndex: layer.index })
      displayOrder += 1
    }
  }
  return result
}

export function layerDisplayItems<T>(
  layer: LayeredRewindLayer<T>,
): Array<LayeredRewindItem<T>> {
  return [
    ...sortLayerItems(layer.items),
    ...sortLayerItems(layer.trimItems),
  ]
}

export function compareLayerRows(
  a?: LayeredRewindSort,
  b?: LayeredRewindSort,
): number {
  if (!a && !b) return 0
  if (!a) return 1
  if (!b) return -1
  return a.displayOrder - b.displayOrder
}

function sortLayerItems<T>(
  items: Array<LayeredRewindItem<T>>,
): Array<LayeredRewindItem<T>> {
  return [...items].sort((a, b) => a.width - b.width || a.sort - b.sort)
}
