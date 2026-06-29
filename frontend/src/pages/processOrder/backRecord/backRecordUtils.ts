import type {
  BackRecordDTO,
  BackRecordFinishDTO,
  BackRecordResultVO,
  BackRecordRollDTO,
  FinishRoll,
  OriginalRoll,
  ProcessOrderDetailVO,
} from '../../../types/processOrder'

export interface RollRecordValues {
  actualGramWeight?: number
  actualWidth?: number
  actualWeight?: number
  remark?: string
}

export interface FinishRecordValues {
  actualWeight?: number
  scrapWeight?: number
  isRemain?: number
  isAbnormal?: number
  abnormalType?: string
  actualRemark?: string
}

export interface BackRecordFormValues {
  operator?: string
  rolls?: Record<string, RollRecordValues>
  finishes?: Record<string, FinishRecordValues>
}

export interface BackRecordMetrics {
  rollCount: number
  finishCount: number
  directShipCount: number
  originalActualTotal: number
  finishActualTotal: number
  scrapTotal: number
  missingRollWeight: number
  missingOfficialFinishWeight: number
}

export interface BackRecordAuthorization {
  operator: string
  releaseReason: string
}

export function activeFinishRolls(detail?: ProcessOrderDetailVO | null): FinishRoll[] {
  return (detail?.finishRolls ?? []).filter((finish) => finish.rollNoStatus !== 3 && finish.sourceType !== 2)
}

export function initialBackRecordValues(detail: ProcessOrderDetailVO): BackRecordFormValues {
  return {
    operator: '',
    rolls: Object.fromEntries(detail.originalRolls.map((roll) => [roll.uuid, rollValues(roll)])),
    finishes: Object.fromEntries(activeFinishRolls(detail).map((finish) => [finish.uuid, finishValues(finish)])),
  }
}

export function buildBackRecordDTO(
  detail: ProcessOrderDetailVO,
  values: BackRecordFormValues,
  authorization?: BackRecordAuthorization,
): BackRecordDTO {
  const finishes = activeFinishRolls(detail).map((finish) => toFinishDTO(finish, values.finishes?.[finish.uuid]))
  return {
    operator: authorization?.operator || values.operator || undefined,
    overToleranceAuthorized: !!authorization,
    releaseReason: authorization?.releaseReason,
    rolls: detail.originalRolls.map((roll) => toRollDTO(roll, values.rolls?.[roll.uuid])),
    finishes: finishes.length > 0 ? finishes : undefined,
  }
}

export function fillRollActuals(detail: ProcessOrderDetailVO): BackRecordFormValues['rolls'] {
  return Object.fromEntries(detail.originalRolls.map((roll) => [roll.uuid, {
    actualGramWeight: roll.actualGramWeight ?? roll.gramWeight,
    actualWidth: roll.actualWidth ?? roll.originalWidth,
    actualWeight: roll.actualWeight ?? nominalRollWeight(roll),
    remark: roll.remark,
  }]))
}

export function fillFinishActuals(detail: ProcessOrderDetailVO): BackRecordFormValues['finishes'] {
  return Object.fromEntries(activeFinishRolls(detail).map((finish) => [finish.uuid, {
    actualWeight: finish.actualWeight ?? (finish.isSpare === 1 ? undefined : finish.estimateWeight),
    scrapWeight: finish.scrapWeight,
    isRemain: finish.isRemain ?? 0,
    isAbnormal: finish.isAbnormal ?? 0,
    abnormalType: finish.abnormalType,
    actualRemark: finish.actualRemark,
  }]))
}

export function buildBackRecordMetrics(
  detail: ProcessOrderDetailVO | null,
  values: BackRecordFormValues,
): BackRecordMetrics {
  const rolls = detail?.originalRolls ?? []
  const finishes = activeFinishRolls(detail)
  return {
    rollCount: rolls.length,
    finishCount: finishes.filter((finish) => finish.isSpare !== 1).length,
    directShipCount: rolls.filter((roll) => roll.processMode === 3).length,
    originalActualTotal: sum(rolls.map((roll) => values.rolls?.[roll.uuid]?.actualWeight ?? roll.actualWeight)),
    finishActualTotal: sum(finishes.map((finish) => values.finishes?.[finish.uuid]?.actualWeight ?? finish.actualWeight)),
    scrapTotal: sum(finishes.map((finish) => values.finishes?.[finish.uuid]?.scrapWeight ?? finish.scrapWeight)),
    missingRollWeight: rolls.filter((roll) => !positive(values.rolls?.[roll.uuid]?.actualWeight ?? roll.actualWeight)).length,
    missingOfficialFinishWeight: finishes.filter((finish) => finish.isSpare !== 1 && !positive(values.finishes?.[finish.uuid]?.actualWeight ?? finish.actualWeight)).length,
  }
}

export function worstRollCheck(result?: BackRecordResultVO | null) {
  const checks = result?.rollChecks ?? []
  return checks.find((check) => check.level === 'BLOCK')
    ?? checks.find((check) => check.level === 'WARN')
    ?? checks[0]
}

function rollValues(roll: OriginalRoll): RollRecordValues {
  return {
    actualGramWeight: roll.actualGramWeight,
    actualWidth: roll.actualWidth,
    actualWeight: roll.actualWeight,
    remark: roll.remark,
  }
}

function finishValues(finish: FinishRoll): FinishRecordValues {
  return {
    actualWeight: finish.actualWeight,
    scrapWeight: finish.scrapWeight,
    isRemain: finish.isRemain,
    isAbnormal: finish.isAbnormal,
    abnormalType: finish.abnormalType,
    actualRemark: finish.actualRemark,
  }
}

function toRollDTO(roll: OriginalRoll, values?: RollRecordValues): BackRecordRollDTO {
  return {
    uuid: roll.uuid,
    actualGramWeight: values?.actualGramWeight,
    actualWidth: values?.actualWidth,
    actualWeight: values?.actualWeight,
    remark: values?.remark,
  }
}

function toFinishDTO(finish: FinishRoll, values?: FinishRecordValues): BackRecordFinishDTO {
  return {
    uuid: finish.uuid,
    actualWeight: values?.actualWeight,
    scrapWeight: values?.scrapWeight,
    isRemain: values?.isRemain,
    isAbnormal: values?.isAbnormal,
    ...(values?.abnormalType ? { abnormalType: values.abnormalType } : {}),
    ...(values?.actualRemark ? { actualRemark: values.actualRemark } : {}),
  }
}

function nominalRollWeight(roll: OriginalRoll): number | undefined {
  if (roll.rollWeight == null) return undefined
  return roll.rollWeight * (roll.pieceNum ?? 1)
}

function positive(value?: number) {
  return value != null && value > 0
}

function sum(values: Array<number | undefined>) {
  return values.reduce<number>((total, value) => total + (value ?? 0), 0)
}
