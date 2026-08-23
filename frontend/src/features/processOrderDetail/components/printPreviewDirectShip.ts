import type {
  FinishProductionVO,
  FinishRoll,
  RollProductionVO,
} from '../../../types/processOrder'
import { formatProductionEstimateKg, formatProductionKg } from '../orderDetailUtils'
import { printFinishSpec } from './printPreviewSpecification'
import { isProductionWeightKnown, productionSourceEstimateWeight } from '../productionSourceWeight'
import type { PrintRouteOutput, PrintRouteStage } from './printPreviewTypes'

export function directShipStage(
  production: RollProductionVO,
  finishRolls: FinishRoll[],
): PrintRouteStage {
  return {
    key: `${production.originalUuid ?? 'roll'}-direct`,
    title: '不加工直发',
    source: '原卷',
    metric: '直接交付',
    requirement: '无需加工；按备注或标注要求处理标签后直接交付。',
    outputs: [directShipOutput(production, matchingFinish(production, finishRolls))],
  }
}

function directShipOutput(
  production: RollProductionVO,
  finish?: FinishRoll,
): PrintRouteOutput {
  const estimateWeight = directShipEstimateWeight(production, finish)
  return {
    key: finish?.uuid ?? `${production.originalUuid ?? 'roll'}-direct-output`,
    finishRollUuid: finish?.uuid,
    name: finish?.finishRollNo || production.rollNo || production.extraNo || '原卷直发',
    spec: printFinishSpec(directSpec(production, finish)),
    weight: estimateWeight == null ? '待称重' : formatProductionEstimateKg(estimateWeight),
    actualWeight: finish?.actualWeight == null
      ? undefined
      : formatProductionKg(finish.actualWeight, production),
    weightValue: estimateWeight,
    width: finish?.finishWidth ?? production.actualWidth ?? production.originalWidth,
    status: 'final',
  }
}

function directShipEstimateWeight(production: RollProductionVO, finish?: FinishRoll) {
  if (finish?.estimateWeight != null && finish.estimateWeight > 0) return finish.estimateWeight
  if (!isProductionWeightKnown(production)) return undefined
  return productionSourceEstimateWeight(production)
}

function matchingFinish(production: RollProductionVO, finishes: FinishRoll[]) {
  const rollNo = production.rollNo || production.extraNo
  return finishes.find((finish) => (
    finish.sourceType === 2
    && finish.rollNoStatus !== 3
    && finish.finishRollNo === rollNo
  ))
}

function directSpec(
  production: RollProductionVO,
  finish?: FinishRoll,
): FinishProductionVO {
  return {
    uuid: finish?.uuid ?? `${production.originalUuid ?? 'roll'}-direct-spec`,
    paperName: finish?.paperName ?? production.paperName,
    gramWeight: finish?.gramWeight ?? production.actualGramWeight ?? production.gramWeight,
    finishWidth: finish?.finishWidth ?? production.actualWidth ?? production.originalWidth,
  }
}
