import type { OriginalRoll, ProcessStep } from '../../types/processOrder'

export interface PricingPreview {
  quantity?: number
  billingBasis?: string
  standardAmount: number
  finalAmount: number
  adjustment: number
}

interface Options {
  step: ProcessStep
  originalRoll?: OriginalRoll
  mode?: number
  billingBasis?: string
  billingQuantity?: number
  billingAmount?: number
  billingUnitPrice?: number
}

export function pricingPreview(options: Options): PricingPreview {
  const { step, mode, billingQuantity, billingAmount, billingUnitPrice } = options
  const standardAmount = step.standardStepAmount ?? step.stepAmount ?? 0
  const basis = options.billingBasis ?? step.billingBasis
  const quantity = isServiceStep(step)
    ? serviceQuantity(basis, options.originalRoll)
      ?? (basis === step.billingBasis ? step.serviceQuantity : undefined)
    : billingQuantity ?? step.standardQuantity
  let finalAmount = step.stepAmount ?? standardAmount
  if (mode === 2 && quantity != null && step.unitPrice != null) {
    finalAmount = Math.round(quantity * step.unitPrice)
  }
  if (mode === 3) finalAmount = billingAmount ?? 0
  if (mode === 4) finalAmount = 0
  if (mode === 1 && isServiceStep(step) && billingUnitPrice != null) {
    finalAmount = Math.round((quantity ?? 0) * billingUnitPrice)
  }
  return {
    quantity,
    billingBasis: basis,
    standardAmount,
    finalAmount,
    adjustment: finalAmount - standardAmount,
  }
}

export function isServiceStep(step?: ProcessStep | null): boolean {
  return step?.stepType === 3 || step?.stepType === 4
}

function serviceQuantity(basis?: string, roll?: OriginalRoll): number | undefined {
  if (!roll) return undefined
  const pieces = roll.pieceNum ?? 1
  if (basis === 'PIECE') return pieces
  if (basis !== 'TON') return undefined
  const weight = positive(roll.actualWeight) ?? positive(roll.totalWeight)
    ?? (positive(roll.rollWeight) == null ? undefined : roll.rollWeight! * pieces)
  return weight == null ? undefined : weight / 1000
}

function positive(value?: number): number | undefined {
  return value != null && value > 0 ? value : undefined
}
