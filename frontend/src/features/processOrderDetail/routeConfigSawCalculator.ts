import type { FinishConfigSpecDTO, ProcessPlanDTO } from '../../types/processOrder'
import type { DetailRouteOutputRow } from './routeConfigModel'
import {
  appendTrimSeed,
  calcTrimWeight,
  roundWeight,
  seedFromSource,
  type RouteOutputSeed,
} from './routeConfigSource'

export function calculateSawOutputSeeds(
  source: DetailRouteOutputRow,
  plan: ProcessPlanDTO,
): RouteOutputSeed[] {
  const allSpecs = plan.finishSpecs ?? []
  const specs = allSpecs.filter((spec) => (spec.itemType ?? 'FINISH') !== 'TRIM')
  const expandedSpecs = specs.flatMap((spec) => (
    Array.from({ length: Math.max(1, spec.count ?? 1) }, () => spec)
  ))
  const trimWidth = sawTrimWidth(source, allSpecs)
  const trimWeightValue = calcTrimWeight(source.estimateWeight, source.finishWidth, trimWidth)
  const finishWeight = Math.max(0, source.estimateWeight - trimWeightValue)
  const widthTotal = expandedSpecs.reduce(
    (sum, spec) => sum + Number(spec.finishWidth ?? 0),
    0,
  )
  let allocated = 0
  const rows = expandedSpecs.map((spec, index) => {
    const estimateWeight = allocatedSawWeight({
      allocated,
      count: expandedSpecs.length,
      index,
      totalWeight: finishWeight,
      width: Number(spec.finishWidth ?? 0),
      widthTotal,
    })
    allocated += estimateWeight
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
  return appendTrimSeed(rows, source, trimWidth, trimWeightValue)
}

interface SawWeightAllocation {
  allocated: number
  count: number
  index: number
  totalWeight: number
  width: number
  widthTotal: number
}

function allocatedSawWeight(options: SawWeightAllocation) {
  if (options.count <= 0 || options.totalWeight <= 0 || options.widthTotal <= 0) return 0
  if (options.index === options.count - 1) {
    return roundWeight(options.totalWeight - options.allocated)
  }
  return roundWeight(options.totalWeight * options.width / options.widthTotal)
}

function sawTrimWidth(source: DetailRouteOutputRow, specs: FinishConfigSpecDTO[]) {
  const explicitTrim = specs
    .filter((spec) => spec.itemType === 'TRIM')
    .reduce(
      (sum, spec) => sum + Number(spec.finishWidth ?? 0) * Math.max(1, spec.count ?? 1),
      0,
    )
  if (explicitTrim > 0) return explicitTrim
  const finishWidth = specs
    .filter((spec) => (spec.itemType ?? 'FINISH') !== 'TRIM')
    .reduce(
      (sum, spec) => sum + Number(spec.finishWidth ?? 0) * Math.max(1, spec.count ?? 1),
      0,
    )
  return Math.max(0, source.finishWidth - finishWidth)
}
