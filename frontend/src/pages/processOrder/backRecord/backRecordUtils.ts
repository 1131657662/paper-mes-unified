import type {
  BackRecordDTO,
  BackRecordFinishDTO,
  BackRecordResultVO,
  BackRecordRollDTO,
  BackRecordStepDTO,
  FinishRoll,
  OriginalRoll,
  ProcessStep,
  ProcessOrderDetailVO,
} from '../../../types/processOrder'
import { buildOnSiteOutputSubmission, toLegacyTrimDTOs, type OnSiteOutputRecordValues } from './backRecordOnSiteOutputModel'
export type { OnSiteOutputRecordValues } from './backRecordOnSiteOutputModel'

export interface RollRecordValues {
  actualGramWeight?: number
  actualWidth?: number
  actualWeight?: number
  remark?: string
}

export interface FinishRecordValues {
  originalUuid?: string
  finishWidth?: number
  finishDiameter?: number
  finishCoreDiameter?: number
  actualWeight?: number
  scrapWeight?: number
  isRemain?: number
  isAbnormal?: number
  abnormalType?: string
  actualRemark?: string
}

export interface StepRecordValues {
  lossWeight?: number
  knifeCount?: number
}

export interface TrimRecordValues {
  originalUuid?: string
  finishWidth?: number
  actualWeight?: number
  actualRemark?: string
}

export interface BackRecordFormValues {
  rolls?: Record<string, RollRecordValues>
  finishes?: Record<string, FinishRecordValues>
  trims?: Record<string, TrimRecordValues[]>
  onSiteOutputs?: Record<string, Array<OnSiteOutputRecordValues | undefined>>
  steps?: Record<string, StepRecordValues>
}

export interface BackRecordAuthorization {
  releaseAdminUsername: string
  releaseAdminPassword: string
  releaseReason: string
}

export interface BackRecordVarianceConfirmation {
  varianceReason: string
}

export function activeFinishRolls(detail?: ProcessOrderDetailVO | null): FinishRoll[] {
  return (detail?.finishRolls ?? []).filter((finish) => finish.rollNoStatus !== 3 && finish.sourceType !== 2)
}

export function initialBackRecordValues(detail: ProcessOrderDetailVO): BackRecordFormValues {
  return {
    rolls: Object.fromEntries(detail.originalRolls.map((roll) => [roll.uuid, rollValues(roll)])),
    finishes: Object.fromEntries(activeFinishRolls(detail).map((finish) => [finish.uuid, finishValues(finish)])),
    trims: {},
    onSiteOutputs: {},
    steps: Object.fromEntries(detail.steps.map((step) => [step.uuid, stepValues(step)])),
  }
}

export function buildBackRecordDTO(
  detail: ProcessOrderDetailVO,
  values: BackRecordFormValues,
  authorization?: BackRecordAuthorization,
  variance?: BackRecordVarianceConfirmation,
): BackRecordDTO {
  const onSite = buildOnSiteOutputSubmission(detail, values.onSiteOutputs)
  const finishes = activeFinishRolls(detail)
    .filter((finish) => !onSite.configuredUuids.has(finish.uuid) && !onSite.managedUuids.has(finish.uuid))
    .map((finish) => toFinishDTO(finish, values.finishes?.[finish.uuid]))
  finishes.push(...onSite.finishes)
  const trims = [...toLegacyTrimDTOs(values.trims), ...onSite.trims]
  return {
    releaseAdminUsername: authorization?.releaseAdminUsername,
    releaseAdminPassword: authorization?.releaseAdminPassword,
    releaseReason: authorization?.releaseReason,
    varianceReason: variance?.varianceReason,
    rolls: detail.originalRolls.map((roll) => toRollDTO(roll, values.rolls?.[roll.uuid])),
    finishes: finishes.length > 0 ? finishes : undefined,
    trims: trims.length > 0 ? trims : undefined,
    steps: detail.steps.map((step) => toStepDTO(step, values.steps?.[step.uuid])),
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
    finishWidth: validWidth(finish.finishWidth),
    finishDiameter: finish.finishDiameter,
    finishCoreDiameter: finish.finishCoreDiameter,
    actualWeight: finish.actualWeight,
    scrapWeight: finish.scrapWeight,
    isRemain: finish.isRemain,
    isAbnormal: finish.isAbnormal,
    abnormalType: finish.abnormalType,
    actualRemark: finish.actualRemark,
  }
}

function stepValues(step: ProcessStep): StepRecordValues {
  return {
    lossWeight: step.lossWeight,
    knifeCount: step.knifeCount,
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
    originalUuid: values?.originalUuid,
    finishWidth: values?.finishWidth,
    finishDiameter: values?.finishDiameter,
    finishCoreDiameter: values?.finishCoreDiameter,
    actualWeight: values?.actualWeight,
    scrapWeight: values?.scrapWeight,
    isRemain: values?.isRemain,
    isAbnormal: values?.isAbnormal,
    ...(values?.abnormalType ? { abnormalType: values.abnormalType } : {}),
    ...(values?.actualRemark ? { actualRemark: values.actualRemark } : {}),
  }
}

function toStepDTO(step: ProcessStep, values?: StepRecordValues): BackRecordStepDTO {
  return {
    uuid: step.uuid,
    lossWeight: values?.lossWeight,
    knifeCount: values?.knifeCount != null && values.knifeCount > 0
      ? values.knifeCount
      : undefined,
  }
}

function nominalRollWeight(roll: OriginalRoll): number | undefined {
  if (roll.rollWeight == null) return undefined
  return roll.rollWeight * (roll.pieceNum ?? 1)
}

function positive(value?: number) {
  return value != null && value > 0
}

function validWidth(value?: number) {
  return positive(value) ? value : undefined
}
