import type { FinishProductionVO, RollProductionVO } from '../../../types/processOrder'
import { allocateIntegerWeight, roundWeightTotal } from '../../../utils/integerWeightAllocation'
import type { CanonicalWeightMap } from './canonicalEstimateWeight'

type WidthPolicy = 'LOSS' | 'ALLOCATE' | 'REMAINDER'

interface FinishGroupWeightOptions {
  finishes: FinishProductionVO[]
  production: RollProductionVO
  sourceWidth?: number
  targetWeight: number
  widthPolicy?: WidthPolicy
}

export function allocateFinishGroupWeights(
  options: FinishGroupWeightOptions,
  result: CanonicalWeightMap,
): void {
  const trims = options.finishes.filter((finish) => finish.isRemain === 1)
  const products = options.finishes.filter((finish) => finish.isRemain !== 1)
  if (products.length === 0) {
    allocateOnlyTrims(trims, options.targetWeight, options.production, result)
    return
  }
  const trimBudget = trimBudgetFor(options, trims, products)
  if (trimBudget == null) {
    options.finishes.forEach((finish) => result.set(finish.uuid, undefined))
    return
  }
  allocateProducts(products, options, trimBudget, result)
  allocateTrims(trims, options.production, trimBudget, result)
}

function allocateOnlyTrims(
  trims: FinishProductionVO[],
  targetWeight: number,
  production: RollProductionVO,
  result: CanonicalWeightMap,
): void {
  const measured = trims.filter(hasMeasuredWeight)
  const unknown = trims.filter((finish) => !hasMeasuredWeight(finish))
  measured.forEach((finish) => result.set(finish.uuid, finish.actualWeight))
  const budget = wholeWeightBudget(Math.max(0, targetWeight - sumMeasuredWeight(measured)))
  allocateUnknown(unknown, budget, production, result)
}

function allocateProducts(
  products: FinishProductionVO[],
  options: FinishGroupWeightOptions,
  trimBudget: number,
  result: CanonicalWeightMap,
): void {
  const measured = products.filter(hasMeasuredWeight)
  const unknown = products.filter((finish) => !hasMeasuredWeight(finish))
  measured.forEach((finish) => result.set(finish.uuid, finish.actualWeight))
  const budget = wholeWeightBudget(options.targetWeight - trimBudget - sumMeasuredWeight(measured))
  allocateUnknown(unknown, budget, options.production, result)
}

function allocateTrims(
  trims: FinishProductionVO[],
  production: RollProductionVO,
  trimBudget: number,
  result: CanonicalWeightMap,
): void {
  const measured = trims.filter(hasMeasuredWeight)
  const unknown = trims.filter((finish) => !hasMeasuredWeight(finish))
  measured.forEach((finish) => result.set(finish.uuid, finish.actualWeight))
  const budget = wholeWeightBudget(trimBudget - sumMeasuredWeight(measured))
  allocateUnknown(unknown, budget, production, result)
}

function allocateUnknown(
  finishes: FinishProductionVO[],
  budget: number | undefined,
  production: RollProductionVO,
  result: CanonicalWeightMap,
): void {
  if (finishes.length > 0 && budget == null) {
    finishes.forEach((finish) => result.set(finish.uuid, undefined))
    return
  }
  allocateIntegerWeight(budget ?? 0, finishes.map((finish) => finishBasis(finish, production)))
    .forEach((weight, index) => result.set(finishes[index]!.uuid, weight))
}

function trimBudgetFor(
  options: FinishGroupWeightOptions,
  trims: FinishProductionVO[],
  products: FinishProductionVO[],
): number | undefined {
  if (trims.length === 0) return 0
  const explicit = explicitTrimBudget(trims)
  const measured = sumMeasuredWeight(trims.filter(hasMeasuredWeight))
  if (trims.every(hasMeasuredWeight) && explicit > 0) return Math.min(options.targetWeight, explicit)
  const widthBudget = trimWidthBudget(options, trims, products)
  if (widthBudget != null) return Math.min(options.targetWeight, Math.max(measured, widthBudget))
  return explicit > 0 ? Math.min(options.targetWeight, explicit) : undefined
}

function trimWidthBudget(
  options: FinishGroupWeightOptions,
  trims: FinishProductionVO[],
  products: FinishProductionVO[],
): number | undefined {
  if (!options.sourceWidth || options.sourceWidth <= 0) return undefined
  if (![...trims, ...products].every((finish) => (finish.finishWidth ?? 0) > 0)) return undefined
  const trimWidth = sumWidth(trims)
  const productWidth = sumWidth(products)
  const differenceWidth = options.widthPolicy === 'REMAINDER'
    ? Math.max(0, options.sourceWidth - productWidth - trimWidth) : 0
  const budgetWidth = trimWidth + differenceWidth
  return budgetWidth > 0
    ? roundWeightTotal(options.targetWeight * budgetWidth / options.sourceWidth)
    : undefined
}

function explicitTrimBudget(trims: FinishProductionVO[]): number {
  const values = trims.map((finish) => isPositiveFinite(finish.actualWeight)
    ? finish.actualWeight : finish.trimWeightShare ?? finish.estimateWeight)
  const total = values.reduce<number>(
    (sum, value) => sum + (isPositiveFinite(value) ? value : 0),
    0,
  )
  return total > 0 ? roundWeightTotal(total) : 0
}

function finishBasis(finish: FinishProductionVO, production: RollProductionVO): number {
  if (production.mainStepType === 1) return Math.max(1, finish.finishWidth ?? 1)
  if (isPositiveFinite(finish.estimateWeight)) return finish.estimateWeight
  return Math.max(1, finish.finishWidth ?? 1)
}

function sumWidth(finishes: FinishProductionVO[]): number {
  return finishes.reduce((sum, finish) => sum + Math.max(0, finish.finishWidth ?? 0), 0)
}

function hasMeasuredWeight(finish: FinishProductionVO): boolean {
  return isPositiveFinite(finish.actualWeight)
}

function sumMeasuredWeight(finishes: FinishProductionVO[]): number {
  return finishes.reduce(
    (sum, finish) => sum + (isPositiveFinite(finish.actualWeight) ? finish.actualWeight : 0),
    0,
  )
}

function wholeWeightBudget(value: number): number | undefined {
  if (!Number.isFinite(value) || value < 0) return undefined
  const rounded = Math.round(value)
  return Math.abs(value - rounded) < 1e-9 ? rounded : undefined
}

function isPositiveFinite(value?: number): value is number {
  return value != null && Number.isFinite(value) && value > 0
}
