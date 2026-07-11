import type { FinishProductionVO, RollProductionVO, StageOutputVO } from '../../../types/processOrder'

export interface LayeredRewindItem<T> {
  item: T
  key: string
  sort: number
  width: number
}

export interface LayeredRewindLayer<T> {
  index: number
  key: string
  label: string
  items: Array<LayeredRewindItem<T>>
  summary: string
  trimItems: Array<LayeredRewindItem<T>>
  trimWidth: number
  usedWidth: number
}

interface SourceItem<T> extends LayeredRewindItem<T> {
  isTrim: boolean
}

export function isLayeredRewind(production: RollProductionVO): boolean {
  return production.mainStepType === 2
    && (production.rewindParams?.some((param) => param.paramMode === 4) ?? false)
}

export function buildFinishLayers(
  production: RollProductionVO,
  finishes: FinishProductionVO[],
): Array<LayeredRewindLayer<FinishProductionVO>> {
  return buildLayers(production, finishes.filter(isVisibleFinish).map((finish, index) => ({
    isTrim: finish.isRemain === 1,
    item: finish,
    key: finish.uuid,
    sort: finish.rowSort ?? index + 1,
    width: finish.finishWidth ?? 0,
  })))
}

export function buildStageOutputLayers(
  production: RollProductionVO,
  outputs: StageOutputVO[],
): Array<LayeredRewindLayer<StageOutputVO>> {
  return buildLayers(production, outputs.filter(isVisibleStageOutput).map((output, index) => ({
    isTrim: isTrimOutput(output),
    item: output,
    key: output.uuid,
    sort: output.outputSort ?? index + 1,
    width: output.finishWidth ?? 0,
  })))
}

export function layerItemMap<T>(layers: Array<LayeredRewindLayer<T>>): Map<string, LayeredRewindLayer<T>> {
  const result = new Map<string, LayeredRewindLayer<T>>()
  for (const layer of layers) {
    for (const row of [...layer.items, ...layer.trimItems]) {
      result.set(row.key, layer)
    }
  }
  return result
}

export function layersSummaryText<T>(layers: Array<LayeredRewindLayer<T>>): string {
  return layers.map((layer) => layer.summary).join('；')
}

function buildLayers<T>(
  production: RollProductionVO,
  sourceItems: Array<SourceItem<T>>,
): Array<LayeredRewindLayer<T>> {
  const sourceWidth = production.originalWidth ?? 0
  if (!isLayeredRewind(production) || sourceWidth <= 0) return []

  const outputs = ordered(sourceItems.filter((item) => !item.isTrim && item.width > 0))
  const trimItems = ordered(sourceItems.filter((item) => item.isTrim))
  const layers = groupOutputsByWidth(sourceWidth, outputs)
  attachTrimRows(layers, trimItems)
  refreshSummaries(layers)
  return layers
}

function groupOutputsByWidth<T>(
  sourceWidth: number,
  outputs: Array<SourceItem<T>>,
): Array<LayeredRewindLayer<T>> {
  const layers: Array<LayeredRewindLayer<T>> = []
  let current: Array<LayeredRewindItem<T>> = []
  let usedWidth = 0

  const closeLayer = () => {
    if (!current.length) return
    const index = layers.length + 1
    layers.push({
      index,
      key: `layer-${index}`,
      label: `第${index}层`,
      items: current,
      summary: '',
      trimItems: [],
      trimWidth: Math.max(0, sourceWidth - usedWidth),
      usedWidth,
    })
    current = []
    usedWidth = 0
  }

  for (const output of outputs) {
    if (current.length && usedWidth + output.width > sourceWidth) closeLayer()
    current.push(output)
    usedWidth += output.width
  }
  closeLayer()
  return layers
}

function attachTrimRows<T>(
  layers: Array<LayeredRewindLayer<T>>,
  trimItems: Array<SourceItem<T>>,
) {
  for (const trim of trimItems) {
    const layer = findTrimLayer(layers, trim.width)
    if (!layer) continue
    layer.trimItems.push(trim)
    if (layer.trimWidth <= 0 && trim.width > 0) layer.trimWidth = trim.width
  }
}

function findTrimLayer<T>(layers: Array<LayeredRewindLayer<T>>, width: number) {
  return layers.find((layer) => layer.trimWidth === width && layer.trimItems.length === 0)
    ?? layers.find((layer) => layer.trimWidth > 0 && layer.trimItems.length === 0)
    ?? layers.find((layer) => layer.trimWidth > 0)
    ?? layers[layers.length - 1]
}

function refreshSummaries<T>(layers: Array<LayeredRewindLayer<T>>) {
  for (const layer of layers) {
    layer.summary = layerSummary(layer)
  }
}

function layerSummary<T>(layer: LayeredRewindLayer<T>) {
  const parts = groupedWidthText(layer.items)
  const trim = layer.trimWidth > 0 ? `修边 ${layer.trimWidth} mm` : ''
  return [layer.label, [parts, trim].filter(Boolean).join(' + ')].filter(Boolean).join(' ')
}

function groupedWidthText<T>(items: Array<LayeredRewindItem<T>>) {
  const groups = new Map<number, number>()
  for (const item of items) {
    groups.set(item.width, (groups.get(item.width) ?? 0) + 1)
  }
  return Array.from(groups.entries())
    .sort(([a], [b]) => a - b)
    .map(([width, count]) => `${width} mm × ${count}`)
    .join(' + ')
}

function isTrimOutput(output: StageOutputVO): boolean {
  return output.isRemain === 1
    || output.outputNo === '切边'
    || output.outputNo === '修边'
    || output.paperName === '切边'
    || output.paperName === '修边'
    || output.paperName === '修边/余料'
    || output.remark === '修边/余料'
}

function isVisibleFinish(finish: FinishProductionVO): boolean {
  return finish.rollNoStatus !== 3 && finish.isSpare !== 1
}

function isVisibleStageOutput(output: StageOutputVO): boolean {
  return output.outputStatus !== 4
}

function ordered<T>(items: Array<SourceItem<T>>) {
  return [...items].sort((a, b) => a.sort - b.sort)
}
