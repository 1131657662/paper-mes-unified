export const WEIGHT_BALANCE_TOLERANCE_KG = 0.001

export function weightGain(originalWeight?: number, finishWeight?: number) {
  return Number(finishWeight ?? 0) - Number(originalWeight ?? 0)
}

export function hasWeightGain(originalWeight?: number, finishWeight?: number) {
  return weightGain(originalWeight, finishWeight) > WEIGHT_BALANCE_TOLERANCE_KG
}
