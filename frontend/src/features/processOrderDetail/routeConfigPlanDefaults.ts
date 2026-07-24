import type {
  FinishConfigSpecDTO,
  ProcessPlanDTO,
  RewindSegmentPlanDTO,
} from '../../types/processOrder'
import {
  STEP_TYPE_REWIND,
  STEP_TYPE_SAW,
  type DetailRouteOutputRow,
  type DetailRoutePriceDefaults,
} from './routeConfigModel'
import { combinedSource, roundPercent, roundWeight } from './routeConfigSource'

export function defaultPlanForSources(
  sources: DetailRouteOutputRow[],
  stepType: number,
  prices: DetailRoutePriceDefaults,
): ProcessPlanDTO {
  const source = combinedSource(sources)
  if (stepType === STEP_TYPE_SAW) return defaultSawPlan(source, prices)
  return defaultRewindPlan(sources, prices)
}

export function defaultRewindSegment(
  source: DetailRouteOutputRow,
  sources: DetailRouteOutputRow[] = [source],
): RewindSegmentPlanDTO {
  return {
    segmentSort: 1,
    segmentRatio: 1,
    targetDiameter: source.finishDiameter,
    finishCoreDiameter: source.finishCoreDiameter ?? 3,
    repeatCount: 1,
    sources: sources.map((row, index) => ({
      originalUuid: row.outputKey,
      shareRatio: roundPercent(row.estimateWeight, source.estimateWeight),
      consumeRatio: 100,
      sourceSort: index + 1,
    })),
    layoutItems: [{ width: source.finishWidth, quantity: 1, itemType: 'FINISH' }],
  }
}

function defaultSawPlan(
  source: DetailRouteOutputRow,
  prices: DetailRoutePriceDefaults,
): ProcessPlanDTO {
  const split = splitOutput(source)
  return {
    processMode: 1,
    mainStepType: STEP_TYPE_SAW,
    knifeCount: 1,
    unitPrice: prices.sawUnitPrice,
    finishSpecs: [
      finishSpec(source, split.firstWidth, split.firstWeight),
      finishSpec(source, split.secondWidth, split.secondWeight),
    ],
  }
}

function defaultRewindPlan(
  sources: DetailRouteOutputRow[],
  prices: DetailRoutePriceDefaults,
): ProcessPlanDTO {
  const source = combinedSource(sources)
  return {
    processMode: 1,
    mainStepType: STEP_TYPE_REWIND,
    rewindMode: sources.length > 1 ? 5 : 2,
    unitPrice: prices.rewindUnitPrice,
    segments: [defaultRewindSegment(source, sources)],
  }
}

function finishSpec(
  source: DetailRouteOutputRow,
  finishWidth: number,
  estimateWeight: number,
): FinishConfigSpecDTO {
  return {
    itemType: 'FINISH',
    count: 1,
    finishWidth,
    estimateWeight,
    finishCoreDiameter: source.finishCoreDiameter,
    finishDiameter: source.finishDiameter,
  }
}

function splitOutput(row: DetailRouteOutputRow) {
  const firstWidth = Math.max(1, Math.floor(row.finishWidth / 2))
  const firstWeight = roundWeight(row.estimateWeight / 2)
  return {
    firstWidth,
    firstWeight,
    secondWidth: Math.max(1, row.finishWidth - firstWidth),
    secondWeight: roundWeight(row.estimateWeight - firstWeight),
  }
}
