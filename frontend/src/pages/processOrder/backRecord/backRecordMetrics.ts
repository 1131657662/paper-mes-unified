import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import { activeFinishRolls, type BackRecordFormValues } from './backRecordUtils'
import { buildOnSiteOutputSubmission } from './backRecordOnSiteOutputModel'
import { storedMeasuredWeight } from './backRecordSourceRolls'
import { buildBackRecordWorkbench } from './backRecordWorkbenchUtils'
import { requiredWeightRollUuids } from './backRecordWeightPolicy'

export interface BackRecordMetrics {
  rollCount: number
  finishCount: number
  directShipCount: number
  serviceOnlyCount: number
  originalActualTotal: number
  originalWeightPending: boolean
  finishActualTotal: number
  trimActualTotal: number
  lossTotal: number
  scrapTotal: number
  missingRollWeight: number
  optionalPendingRollWeight: number
  missingOfficialFinishWeight: number
  missingOnSiteFinishWidth: number
  missingTrimData: number
}

export function buildBackRecordMetrics(
  detail: ProcessOrderDetailVO | null,
  values: BackRecordFormValues,
): BackRecordMetrics {
  const rolls = detail?.originalRolls ?? []
  const requiredRollUuids = detail
    ? requiredWeightRollUuids(buildBackRecordWorkbench(detail).items)
    : new Set<string>()
  const onSite = detail ? buildOnSiteOutputSubmission(detail, values.onSiteOutputs) : null
  const finishes = activeFinishRolls(detail).filter((finish) => !onSite?.configuredUuids.has(finish.uuid))
  const adjustments = Object.values(values.finishAdjustments ?? {})
  const notProduced = new Set(adjustments.flatMap((adjustment) => adjustment.plannedFinishUuids
    .filter((uuid) => !adjustment.producedFinishUuids.includes(uuid))))
  const added = adjustments.flatMap((adjustment) => adjustment.added ?? [])
  const trims = Object.values(values.trims ?? {}).flat()
  const products = finishes.filter((finish) => finish.isRemain !== 1 && !notProduced.has(finish.uuid))
  const remains = finishes.filter((finish) => finish.isRemain === 1)
  const outputProducts = onSite?.finishes ?? []
  const outputTrims = onSite?.trims ?? []
  return {
    rollCount: rolls.length,
    finishCount: products.filter((finish) => finish.isSpare !== 1).length + added.length + outputProducts.length,
    directShipCount: rolls.filter((roll) => roll.processMode === 3).length,
    serviceOnlyCount: rolls.filter((roll) => roll.processMode === 4).length,
    originalActualTotal: sum(rolls.map((roll) => measuredRollWeight(roll, values))),
    originalWeightPending: rolls.some((roll) => !positive(measuredRollWeight(roll, values))),
    finishActualTotal: sum(products.map((finish) => values.finishes?.[finish.uuid]?.actualWeight ?? finish.actualWeight))
      + sum(added.map((finish) => values.finishes?.[finish.uuid]?.actualWeight))
      + sum(outputProducts.map((finish) => finish.actualWeight)),
    trimActualTotal: sum(remains.map((finish) => values.finishes?.[finish.uuid]?.actualWeight ?? finish.actualWeight))
      + sum(trims.map((trim) => trim.actualWeight)) + sum(outputTrims.map((trim) => trim.actualWeight)),
    lossTotal: sum((detail?.steps ?? []).map((step) => values.steps?.[step.uuid]?.lossWeight ?? step.lossWeight)),
    scrapTotal: sum(finishes.map((finish) => values.finishes?.[finish.uuid]?.scrapWeight ?? finish.scrapWeight))
      + sum(added.map((finish) => values.finishes?.[finish.uuid]?.scrapWeight)),
    missingRollWeight: rolls.filter((roll) => requiredRollUuids.has(roll.uuid)
      && !positive(measuredRollWeight(roll, values))).length,
    optionalPendingRollWeight: rolls.filter((roll) => !requiredRollUuids.has(roll.uuid)
      && !positive(measuredRollWeight(roll, values))).length,
    missingOfficialFinishWeight: products.filter((finish) => finish.isSpare !== 1 && !positive(values.finishes?.[finish.uuid]?.actualWeight ?? finish.actualWeight)).length
      + added.filter((finish) => !positive(values.finishes?.[finish.uuid]?.actualWeight)).length
      + outputProducts.filter((finish) => !positive(finish.actualWeight)).length,
    missingOnSiteFinishWidth: outputProducts.filter((finish) => !positive(finish.finishWidth)).length,
    missingTrimData: remains.filter((finish) => finish.isSpare !== 1 && !positive(values.finishes?.[finish.uuid]?.actualWeight ?? finish.actualWeight)).length
      + trims.filter((trim) => !positive(trim.finishWidth) || !positive(trim.actualWeight)).length
      + outputTrims.filter((trim) => !positive(trim.finishWidth) || !positive(trim.actualWeight)).length,
  }
}

function positive(value?: number) {
  return value != null && value > 0
}

function measuredRollWeight(
  roll: ProcessOrderDetailVO['originalRolls'][number],
  values: BackRecordFormValues,
): number | undefined {
  const entry = values.rolls?.[roll.uuid]
  if (entry?.weightEntryMode) {
    return entry.weightEntryMode === 'MEASURED' && positive(entry.actualWeight)
      ? entry.actualWeight
      : undefined
  }
  return entry?.actualWeight ?? storedMeasuredWeight(roll)
}

function sum(values: Array<number | undefined>) {
  return values.reduce<number>((total, value) => total + (value ?? 0), 0)
}
