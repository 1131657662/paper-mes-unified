import {
  buildStageOutputLayers,
  layerItemMap,
  type LayeredRewindLayer,
} from '../../../components/processOrder/shared/layeredRewindView'
import {
  compareLayerRows,
  layerItemSortMap,
  type LayeredRewindSort,
} from '../../../components/processOrder/shared/layeredRewindOrder'
import { sortStageOutputs } from '../../../components/processOrder/shared/outputOrder'
import type { ProcessStep, RollProductionVO, StageOutputVO } from '../../../types/processOrder'
import type { PrintRouteOutput } from './printPreviewTypes'

interface PrintRouteSortContext<T> {
  fallbackTrimLayer?: LayeredRewindLayer<T>
  sortMap: Map<string, LayeredRewindSort>
}

export function layeredRouteOutputs(
  production: RollProductionVO,
  rows: PrintRouteOutput[],
  outputs: StageOutputVO[],
  step?: ProcessStep,
): PrintRouteOutput[] {
  if ((step?.stepType ?? outputs[0]?.sourceStepType) !== 2) {
    return sortRowsByStageOutputs(production, rows, outputs)
  }
  const layers = buildStageOutputLayers(production, outputs)
  return layers.length
    ? withPrintLayerTexts(rows, layers)
    : sortRowsByStageOutputs(production, rows, outputs)
}

export function withPrintLayerTexts<T>(
  rows: PrintRouteOutput[],
  layers: Array<LayeredRewindLayer<T>>,
): PrintRouteOutput[] {
  if (!layers.length) return rows
  const map = layerItemMap(layers)
  const sortMap = layerItemSortMap(layers)
  const fallbackTrimLayer = layers.find((layer) => layer.trimWidth > 0)
  const sortContext = { fallbackTrimLayer, sortMap }
  return rows.map((row) => {
    const layer = map.get(row.key) ?? (row.status === 'trim' ? fallbackTrimLayer : undefined)
    return layer ? { ...row, layerText: layer.label } : row
  }).sort((a, b) => comparePrintRouteOutputs(a, b, sortContext))
}

function sortRowsByStageOutputs(
  production: RollProductionVO,
  rows: PrintRouteOutput[],
  outputs: StageOutputVO[],
) {
  const order = new Map(
    sortStageOutputs(production, outputs).map((output, index) => [output.uuid, index]),
  )
  return [...rows].sort((a, b) => (order.get(a.key) ?? 0) - (order.get(b.key) ?? 0))
}

function comparePrintRouteOutputs<T>(
  a: PrintRouteOutput,
  b: PrintRouteOutput,
  context: PrintRouteSortContext<T>,
) {
  return compareLayerRows(outputSortInfo(a, context), outputSortInfo(b, context))
    || (a.width ?? 0) - (b.width ?? 0)
    || a.name.localeCompare(b.name)
}

function outputSortInfo<T>(
  row: PrintRouteOutput,
  context: PrintRouteSortContext<T>,
) {
  return context.sortMap.get(row.key)
    ?? (row.status === 'trim' && context.fallbackTrimLayer
      ? fallbackTrimSort(context.sortMap, context.fallbackTrimLayer.index)
      : undefined)
}

function fallbackTrimSort(
  sortMap: Map<string, LayeredRewindSort>,
  layerIndex: number,
): LayeredRewindSort {
  const layerOrders = Array.from(sortMap.values())
    .filter((item) => item.layerIndex === layerIndex)
  const maxOrder = Math.max(0, ...layerOrders.map((item) => item.displayOrder))
  return { displayOrder: maxOrder + 0.5, layerIndex }
}
