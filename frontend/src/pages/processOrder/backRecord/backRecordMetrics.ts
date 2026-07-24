import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import { activeFinishRolls, type BackRecordFormValues } from './backRecordUtils'
import { buildOnSiteOutputSubmission } from './backRecordOnSiteOutputModel'

export interface BackRecordMetrics {
  rollCount: number
  finishCount: number
  directShipCount: number
  serviceOnlyCount: number
  originalActualTotal: number
  finishActualTotal: number
  trimActualTotal: number
  lossTotal: number
  scrapTotal: number
  missingRollWeight: number
  missingOfficialFinishWeight: number
  missingOnSiteFinishWidth: number
  missingTrimData: number
}

export function buildBackRecordMetrics(
  detail: ProcessOrderDetailVO | null,
  values: BackRecordFormValues,
): BackRecordMetrics {
  const rolls = detail?.originalRolls ?? []
  const onSite = detail ? buildOnSiteOutputSubmission(detail, values.onSiteOutputs) : null
  const finishes = activeFinishRolls(detail).filter((finish) => !onSite?.configuredUuids.has(finish.uuid))
  const trims = Object.values(values.trims ?? {}).flat()
  const products = finishes.filter((finish) => finish.isRemain !== 1)
  const remains = finishes.filter((finish) => finish.isRemain === 1)
  const outputProducts = onSite?.finishes ?? []
  const outputTrims = onSite?.trims ?? []
  return {
    rollCount: rolls.length,
    finishCount: products.filter((finish) => finish.isSpare !== 1).length + outputProducts.length,
    directShipCount: rolls.filter((roll) => roll.processMode === 3).length,
    serviceOnlyCount: rolls.filter((roll) => roll.processMode === 4).length,
    originalActualTotal: sum(rolls.map((roll) => values.rolls?.[roll.uuid]?.actualWeight ?? roll.actualWeight)),
    finishActualTotal: sum(products.map((finish) => values.finishes?.[finish.uuid]?.actualWeight ?? finish.actualWeight))
      + sum(outputProducts.map((finish) => finish.actualWeight)),
    trimActualTotal: sum(remains.map((finish) => values.finishes?.[finish.uuid]?.actualWeight ?? finish.actualWeight))
      + sum(trims.map((trim) => trim.actualWeight)) + sum(outputTrims.map((trim) => trim.actualWeight)),
    lossTotal: sum((detail?.steps ?? []).map((step) => values.steps?.[step.uuid]?.lossWeight ?? step.lossWeight)),
    scrapTotal: sum(finishes.map((finish) => values.finishes?.[finish.uuid]?.scrapWeight ?? finish.scrapWeight)),
    missingRollWeight: rolls.filter((roll) => !positive(values.rolls?.[roll.uuid]?.actualWeight ?? roll.actualWeight)).length,
    missingOfficialFinishWeight: products.filter((finish) => finish.isSpare !== 1 && !positive(values.finishes?.[finish.uuid]?.actualWeight ?? finish.actualWeight)).length
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

function sum(values: Array<number | undefined>) {
  return values.reduce<number>((total, value) => total + (value ?? 0), 0)
}
