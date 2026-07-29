import type {
  FinishProductionVO,
  FinishRoll,
  RollProductionVO,
} from '../../../types/processOrder'
import { formatProductionKg } from '../orderDetailUtils'
import { printFinishSpec } from './printPreviewSpecification'
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
  const estimateWeight = finish?.estimateWeight
    ?? production.actualWeight
    ?? (production.rollWeight ?? 0) * (production.pieceNum ?? 1)
  return {
    key: finish?.uuid ?? `${production.originalUuid ?? 'roll'}-direct-output`,
    finishRollUuid: finish?.uuid,
    name: finish?.finishRollNo || production.rollNo || production.extraNo || '原卷直发',
    spec: printFinishSpec(directSpec(production, finish)),
    weight: formatProductionKg(estimateWeight, production),
    actualWeight: finish?.actualWeight == null
      ? undefined
      : formatProductionKg(finish.actualWeight, production),
    weightValue: estimateWeight,
    width: finish?.finishWidth ?? production.actualWidth ?? production.originalWidth,
    status: 'final',
  }
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
