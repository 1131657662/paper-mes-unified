import type {
  ProcessPlanDTO,
  RewindLayoutItemPlanDTO,
  RewindSegmentPlanDTO,
} from '../../types/processOrder'
import type { DetailRouteOutputRow } from './routeConfigModel'
import { defaultRewindSegment } from './routeConfigPlanDefaults'
import {
  appendTrimSeed,
  calcTrimWeight,
  combinedSource,
  roundWeight,
  type RouteOutputSeed,
} from './routeConfigSource'

interface RewindSeedInput extends Omit<RouteOutputSeed, 'estimateWeight'> {
  basis: number
}

export function calculateRewindOutputSeeds(
  sources: DetailRouteOutputRow[],
  plan: ProcessPlanDTO,
): RouteOutputSeed[] {
  const source = combinedSource(sources)
  const segments = plan.segments?.length ? plan.segments : [defaultRewindSegment(source)]
  const ratios = rewindSegmentRatios(sources, segments, plan.rewindMode)
  const totalWeight = rewindTotalWeight(sources, segments, plan.rewindMode)
  const trimWidth = rewindTrimWidth(segments, source.finishWidth, ratios, plan.rewindMode)
  const inputs = segments.flatMap((segment, index) => (
    rewindSegmentSeedInputs(source, segment, ratios[index] ?? 0, plan.rewindMode)
  ))
  const rows = allocateRewindSeeds(inputs, totalWeight, source.finishWidth, trimWidth)
  const trimWeight = calcTrimWeight(totalWeight, source.finishWidth, trimWidth)
  return appendTrimSeed(rows, source, trimWidth, trimWeight)
}

function rewindSegmentSeedInputs(
  source: DetailRouteOutputRow,
  segment: RewindSegmentPlanDTO,
  segmentRatio: number,
  rewindMode?: number,
): RewindSeedInput[] {
  const items = (segment.layoutItems ?? []).filter((item) => item.itemType !== 'TRIM')
  const repeatCount = Math.max(1, Number(segment.repeatCount ?? 1))
  const repeatedRatio = segmentRatio / repeatCount
  return items.flatMap((item) => (
    Array.from({ length: repeatCount * Math.max(1, item.quantity ?? 1) }, () => ({
      basis: rewindSeedBasis(source, segment, item, repeatedRatio, rewindMode),
      finishCoreDiameter: segment.finishCoreDiameter
        ?? firstLayerCoreDiameter(item.layers)
        ?? source.finishCoreDiameter,
      finishDiameter: segment.targetDiameter
        ?? maxLayerOutDiameter(item.layers)
        ?? source.finishDiameter,
      finishWidth: item.width,
      gramWeight: source.gramWeight,
      paperName: source.paperName,
    }))
  ))
}

function allocateRewindSeeds(
  inputs: RewindSeedInput[],
  totalWeight: number,
  originalWidth: number,
  trimWidth: number,
): RouteOutputSeed[] {
  if (!inputs.length) return []
  const trimWeight = originalWidth > 0 ? totalWeight * trimWidth / originalWidth : 0
  const allocatableWeight = totalWeight - trimWeight
  const basisTotal = inputs.reduce((sum, input) => sum + input.basis, 0)
  let allocated = 0
  return inputs.map((input, index) => {
    const estimateWeight = index === inputs.length - 1
      ? roundWeight(allocatableWeight - allocated)
      : roundWeight(basisTotal > 0
        ? allocatableWeight * input.basis / basisTotal
        : allocatableWeight / inputs.length)
    allocated += estimateWeight
    return { ...input, estimateWeight }
  })
}

function rewindSeedBasis(
  source: DetailRouteOutputRow,
  segment: RewindSegmentPlanDTO,
  item: RewindLayoutItemPlanDTO,
  ratio: number,
  rewindMode?: number,
) {
  const finishWidth = item.width
  const layeredBasis = layoutLayerArea(item.layers)
  if (rewindMode === 4 && layeredBasis > 0) return layeredBasis * ratio
  const area = layerArea(segment.targetDiameter, segment.finishCoreDiameter)
  if (rewindMode === 2) return (area > 0 ? area : finishWidth) * ratio
  if (rewindMode === 3 || rewindMode === 4) {
    return (area > 0 ? area : source.finishWidth)
      * safeDivide(finishWidth, source.finishWidth)
      * ratio
  }
  return finishWidth * ratio
}

function rewindTrimWidth(
  segments: RewindSegmentPlanDTO[],
  originalWidth: number,
  ratios: number[],
  rewindMode?: number,
) {
  if (rewindMode === 2) return 0
  return segments.reduce(
    (sum, segment, index) => sum + segmentTrimWidth(segment, originalWidth) * (ratios[index] ?? 0),
    0,
  )
}

function rewindSegmentRatios(
  sources: DetailRouteOutputRow[],
  segments: RewindSegmentPlanDTO[],
  rewindMode?: number,
) {
  if (hasSourceConsumption(segments, rewindMode)) {
    const total = totalConsumedWeight(sources, segments)
    if (total > 0) {
      return segments.map((segment) => safeDivide(segmentConsumedWeight(sources, segment), total))
    }
  }
  const totalRatio = segments.reduce((sum, segment) => sum + Number(segment.segmentRatio ?? 1), 0) || 1
  return segments.map((segment) => Number(segment.segmentRatio ?? 1) / totalRatio)
}

function rewindTotalWeight(
  sources: DetailRouteOutputRow[],
  segments: RewindSegmentPlanDTO[],
  rewindMode?: number,
) {
  if (!hasSourceConsumption(segments, rewindMode)) return combinedSource(sources).estimateWeight
  const consumed = totalConsumedWeight(sources, segments)
  return consumed > 0 ? consumed : combinedSource(sources).estimateWeight
}

function hasSourceConsumption(segments: RewindSegmentPlanDTO[], rewindMode?: number) {
  return rewindMode === 5
    && segments.some((segment) => segment.sources?.some((source) => source.consumeRatio != null))
}

function totalConsumedWeight(
  sources: DetailRouteOutputRow[],
  segments: RewindSegmentPlanDTO[],
) {
  return segments.reduce((sum, segment) => sum + segmentConsumedWeight(sources, segment), 0)
}

function segmentConsumedWeight(
  sources: DetailRouteOutputRow[],
  segment: RewindSegmentPlanDTO,
) {
  return (segment.sources ?? []).reduce((sum, source) => {
    const row = sources.find((item) => item.outputKey === source.originalUuid)
    const ratio = Number(source.consumeRatio ?? source.shareRatio ?? 0) / 100
    return sum + (row?.estimateWeight ?? 0) * ratio
  }, 0)
}

function segmentTrimWidth(segment: RewindSegmentPlanDTO, originalWidth: number) {
  const explicitTrim = layoutWidth(segment, 'TRIM')
  if (explicitTrim > 0) return explicitTrim
  return Math.max(0, originalWidth - layoutWidth(segment, 'FINISH'))
}

function layoutWidth(segment: RewindSegmentPlanDTO, type: 'FINISH' | 'TRIM') {
  return (segment.layoutItems ?? [])
    .filter((item) => (item.itemType ?? 'FINISH') === type)
    .reduce((sum, item) => sum + item.width * Math.max(1, Number(item.quantity ?? 1)), 0)
}

function layerArea(targetDiameter?: number, coreDiameter?: number) {
  if (!targetDiameter || !coreDiameter) return 0
  const outRadius = targetDiameter * 25.4 / 2
  const coreRadius = coreDiameter * 25.4 / 2
  return Math.PI * (outRadius * outRadius - coreRadius * coreRadius)
}

function layoutLayerArea(layers?: { outDiameter?: number; coreDiameter?: number }[]) {
  return (layers ?? []).reduce(
    (sum, layer) => sum + layerArea(layer.outDiameter, layer.coreDiameter),
    0,
  )
}

function maxLayerOutDiameter(layers?: { outDiameter?: number }[]) {
  return Math.max(0, ...(layers ?? []).map((layer) => Number(layer.outDiameter ?? 0))) || undefined
}

function firstLayerCoreDiameter(layers?: { coreDiameter?: number }[]) {
  return layers?.find((layer) => Number(layer.coreDiameter ?? 0) > 0)?.coreDiameter
}

function safeDivide(value: number, divisor: number) {
  return divisor ? value / divisor : 0
}
