import type {
  FinishProductionVO,
  OriginalRoll,
  RollProductionVO,
  StageOutputVO,
} from '../../types/processOrder'
import {
  STEP_TYPE_REWIND,
  type DetailRouteOutputRow,
} from './routeConfigModel'
import { finishWeight, rollTotalWeight } from './routeConfigSource'

const ROLL_NO_VOID = 3
const OUTPUT_CONSUMED = 2
const OUTPUT_VOID = 4

export function buildFirstRouteOutputs(
  roll: OriginalRoll,
  production: RollProductionVO | undefined,
  baseStageLevel: number,
  useCurrentOutputs: boolean,
): DetailRouteOutputRow[] {
  const allStageOutputs = activeStageOutputs(production?.stageOutputs)
  const stageOutputs = routeStageOutputs(production?.stageOutputs, useCurrentOutputs)
  const rows = [
    ...stageOutputs.map((output, index) => (
      stageOutputRow(roll, output, index, baseStageLevel)
    )),
    ...unlinkedFinishRows(roll, production, baseStageLevel, allStageOutputs),
  ]
  return rows.length ? rows : [originalAsOutput(roll, baseStageLevel)]
}

export function resolveFirstRouteStepType(
  roll: OriginalRoll,
  production?: RollProductionVO,
) {
  return production?.steps?.find((step) => step.isMain === 1)?.stepType
    ?? production?.steps?.[0]?.stepType
    ?? roll.mainStepType
    ?? STEP_TYPE_REWIND
}

function unlinkedFinishRows(
  roll: OriginalRoll,
  production: RollProductionVO | undefined,
  baseStageLevel: number,
  stageOutputs: StageOutputVO[],
): DetailRouteOutputRow[] {
  const linkedKeys = linkedFinishKeys(stageOutputs)
  const stageLevel = stageOutputs.length ? 1 : baseStageLevel
  return activeFinishes(production?.finishes ?? [])
    .filter((finish) => !finishLinkedToStageOutput(finish, linkedKeys))
    .map((finish, index) => finishOutputRow(roll, finish, index, stageLevel))
}

function finishOutputRow(
  roll: OriginalRoll,
  finish: FinishProductionVO,
  index: number,
  stageLevel: number,
): DetailRouteOutputRow {
  return {
    estimateWeight: finishWeight(
      roll,
      finish.estimateWeight ?? finish.actualWeight,
      finish.finishWidth,
    ),
    finishCoreDiameter: finish.finishCoreDiameter,
    finishDiameter: finish.finishDiameter,
    finishRollNo: finish.finishRollNo,
    finishWidth: Number(finish.finishWidth ?? roll.originalWidth ?? 1),
    gramWeight: finish.gramWeight ?? roll.gramWeight,
    label: `${stageLabel(stageLevel)}产物 ${index + 1}`,
    outputKey: finishOutputKey(finish, index),
    paperName: finish.paperName ?? roll.paperName,
    sourceRollNo: finish.finishRollNo,
    stageLevel,
  }
}

function routeStageOutputs(
  outputs: StageOutputVO[] | undefined,
  useCurrentOutputs: boolean,
) {
  const active = activeStageOutputs(outputs)
  if (useCurrentOutputs) return active.filter((output) => output.outputStatus !== OUTPUT_CONSUMED)
  return active.filter((output) => (output.stageLevel ?? 1) === 1)
}

function activeStageOutputs(outputs: StageOutputVO[] | undefined) {
  return (outputs ?? []).filter((output) => output.outputStatus !== OUTPUT_VOID)
}

function linkedFinishKeys(outputs: StageOutputVO[]) {
  const keys = new Set<string>()
  for (const output of outputs) {
    if (output.finishRollUuid) keys.add(output.finishRollUuid)
    if (output.outputNo) keys.add(output.outputNo)
  }
  return keys
}

function finishLinkedToStageOutput(finish: FinishProductionVO, linkedKeys: Set<string>) {
  return Boolean(
    (finish.uuid && linkedKeys.has(finish.uuid))
    || (finish.finishRollNo && linkedKeys.has(finish.finishRollNo)),
  )
}

function stageOutputRow(
  roll: OriginalRoll,
  output: StageOutputVO,
  index: number,
  baseStageLevel: number,
): DetailRouteOutputRow {
  const stageLevel = output.stageLevel ?? baseStageLevel
  const isRemain = isTrimStageOutput(output) ? 1 : undefined
  return {
    estimateWeight: finishWeight(
      roll,
      output.estimateWeight ?? output.actualWeight,
      output.finishWidth,
    ),
    finishCoreDiameter: output.finishCoreDiameter,
    finishDiameter: output.finishDiameter,
    finishWidth: Number(output.finishWidth ?? roll.originalWidth ?? 1),
    gramWeight: output.gramWeight ?? roll.gramWeight,
    isRemain,
    label: isRemain === 1
      ? `${stageLabel(stageLevel)}修边`
      : `${stageLabel(stageLevel)}产物 ${output.outputSort ?? index + 1}`,
    outputKey: output.outputNo || output.uuid,
    parentOutputUuid: output.parentOutputUuid,
    paperName: output.paperName ?? roll.paperName,
    sourceStepType: output.sourceStepType,
    sourceOutputKey: output.outputNo,
    sourceRollNo: output.outputNo,
    sourceSummary: output.sourceSummary,
    stageLevel,
  }
}

function isTrimStageOutput(output: StageOutputVO) {
  return output.isRemain === 1
    || output.outputNo === '切边'
    || output.outputNo === '修边'
    || output.paperName === '切边'
    || output.paperName === '修边'
    || output.paperName === '修边/余料'
    || output.remark === '修边/余料'
}

function activeFinishes(finishes: FinishProductionVO[]) {
  return finishes.filter((finish) => finish.rollNoStatus !== ROLL_NO_VOID && finish.isRemain !== 1)
}

function finishOutputKey(finish: FinishProductionVO, index: number) {
  return finish.finishRollNo || (finish.uuid ? `F:${finish.uuid}` : `S1-F${index + 1}`)
}

function originalAsOutput(roll: OriginalRoll, stageLevel: number): DetailRouteOutputRow {
  return {
    estimateWeight: rollTotalWeight(roll),
    finishCoreDiameter: roll.coreDiameter,
    finishDiameter: roll.originalDiameter,
    finishWidth: Number(roll.originalWidth ?? 1),
    gramWeight: roll.gramWeight,
    label: `${stageLabel(stageLevel)}产物 1`,
    outputKey: 'S1-F1',
    paperName: roll.paperName,
    sourceRollNo: roll.rollNo || roll.extraNo,
    stageLevel,
  }
}

function stageLabel(stageLevel: number) {
  return stageLevel <= 1 ? '首道' : `第${stageLevel}段`
}
