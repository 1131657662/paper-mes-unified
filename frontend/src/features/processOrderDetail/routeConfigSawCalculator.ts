import type { FinishConfigSpecDTO, ProcessPlanDTO } from '../../types/processOrder'
import type { DetailRouteOutputRow } from './routeConfigModel'
import {
  appendTrimSeed,
  calcTrimWeight,
  seedFromSource,
  type RouteOutputSeed,
} from './routeConfigSource'
import { allocateIntegerWeight } from '../../utils/integerWeightAllocation'

type SawWidthAccounting = {
  finishWidth: number
  explicitTrimWidth: number
  differenceWidth: number
  outputTrimWidth: number
  trimWeight: number
  lossWeight: number
}

export function calculateSawOutputSeeds(
  source: DetailRouteOutputRow,
  plan: ProcessPlanDTO,
): RouteOutputSeed[] {
  const allSpecs = plan.finishSpecs ?? []
  const specs = allSpecs.filter((spec) => (spec.itemType ?? 'FINISH') !== 'TRIM')
  const expandedSpecs = specs.flatMap((spec) => (
    Array.from({ length: Math.max(1, spec.count ?? 1) }, () => spec)
  ))
  const accounting = sawWidthAccounting(source, allSpecs, plan.widthDifferencePolicy)
  const finishWeight = Math.max(
    0,
    source.estimateWeight - accounting.trimWeight - accounting.lossWeight,
  )
  const weights = allocateIntegerWeight(
    finishWeight,
    expandedSpecs.map((spec) => Math.max(1, Number(spec.finishWidth ?? source.finishWidth))),
  )
  const rows = expandedSpecs.map((spec, index) => {
    const estimateWeight = weights[index] ?? 0
    return {
      estimateWeight,
      finishCoreDiameter: spec.finishCoreDiameter ?? source.finishCoreDiameter,
      finishDiameter: spec.finishDiameter ?? source.finishDiameter,
      finishWidth: Number(spec.finishWidth ?? source.finishWidth),
      gramWeight: source.gramWeight,
      paperName: source.paperName,
    }
  })
  if (!rows.length) return [seedFromSource(source)]
  return appendTrimSeed(rows, source, accounting.outputTrimWidth, accounting.trimWeight)
}

function sawWidthAccounting(
  source: DetailRouteOutputRow,
  specs: FinishConfigSpecDTO[],
  policy: ProcessPlanDTO['widthDifferencePolicy'],
): SawWidthAccounting {
  const explicitTrimWidth = specs
    .filter((spec) => spec.itemType === 'TRIM')
    .reduce(
      (sum, spec) => sum + Number(spec.finishWidth ?? 0) * Math.max(1, spec.count ?? 1),
      0,
    )
  const finishWidth = specs
    .filter((spec) => (spec.itemType ?? 'FINISH') !== 'TRIM')
    .reduce(
      (sum, spec) => sum + Number(spec.finishWidth ?? 0) * Math.max(1, spec.count ?? 1),
      0,
    )
  const differenceWidth = Math.max(0, source.finishWidth - finishWidth - explicitTrimWidth)
  const normalizedPolicy = policy ?? 'REMAINDER'
  const outputTrimWidth = explicitTrimWidth + (
    normalizedPolicy === 'REMAINDER' ? differenceWidth : 0
  )
  const trimWeight = calcTrimWeight(source.estimateWeight, source.finishWidth, outputTrimWidth)
  const lossWeight = normalizedPolicy === 'LOSS'
    ? calcTrimWeight(source.estimateWeight, source.finishWidth, differenceWidth)
    : 0
  return {
    finishWidth,
    explicitTrimWidth,
    differenceWidth,
    outputTrimWidth,
    trimWeight,
    lossWeight,
  }
}
