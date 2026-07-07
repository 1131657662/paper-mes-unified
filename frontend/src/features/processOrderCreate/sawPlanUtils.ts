import type { FinishConfigSpecDTO } from '../../types/processOrder'

export interface SawPlanStats {
  productWidth: number
  implicitTrimWidth: number
  trimWidth: number
  usedWidth: number
  remainingWidth: number
  finishCount: number
  trimRows: number
  knifeCount: number
  usedPercent: number
}

export function calcSawPlanStats(specs: FinishConfigSpecDTO[], originalWidth: number): SawPlanStats {
  const productWidth = sumWidth(specs, false)
  const trimWidth = sumWidth(specs, true)
  const implicitTrimWidth = trimWidth > 0 ? 0 : Math.max(0, originalWidth - productWidth)
  const finishCount = specs.filter((spec) => !isTrimSpec(spec)).reduce((sum, spec) => sum + safeCount(spec), 0)
  const trimRows = specs.filter(isTrimSpec).reduce((sum, spec) => sum + safeCount(spec), 0)
  const usedWidth = productWidth + trimWidth
  const effectiveTrimWidth = trimWidth > 0 ? trimWidth : implicitTrimWidth
  return {
    productWidth,
    implicitTrimWidth,
    trimWidth,
    usedWidth,
    finishCount,
    trimRows,
    remainingWidth: originalWidth - usedWidth,
    knifeCount: finishCount <= 0 ? 0 : Math.max(0, finishCount - 1) + (effectiveTrimWidth > 0 ? 1 : 0),
    usedPercent: originalWidth > 0 ? Math.min(100, Math.round((usedWidth / originalWidth) * 100)) : 0,
  }
}

export function normalizeSawSpecs(specs: FinishConfigSpecDTO[]): FinishConfigSpecDTO[] {
  return specs.map((spec) => ({
    ...spec,
    itemType: spec.itemType ?? 'FINISH',
    count: spec.count ?? 1,
  }))
}

export function isTrimSpec(spec: FinishConfigSpecDTO): boolean {
  return spec.itemType === 'TRIM'
}

function sumWidth(specs: FinishConfigSpecDTO[], trim: boolean): number {
  return specs
    .filter((spec) => isTrimSpec(spec) === trim)
    .reduce((sum, spec) => sum + (spec.finishWidth ?? 0) * safeCount(spec), 0)
}

function safeCount(spec: FinishConfigSpecDTO): number {
  return spec.count ?? 1
}
