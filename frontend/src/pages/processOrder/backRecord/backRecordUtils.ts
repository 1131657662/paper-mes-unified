import type {
  BackRecordDTO,
  BackRecordFinishDTO,
  BackRecordResultVO,
  BackRecordRollDTO,
  BackRecordStepDTO,
  FinishRoll,
  BackRecordFinishAction,
  BackRecordFinishAdjustmentValues,
  OriginalRoll,
  ProcessStep,
  ProcessOrderDetailVO,
  WeightEntryMode,
} from '../../../types/processOrder'
import { buildOnSiteOutputSubmission, toLegacyTrimDTOs, type OnSiteOutputRecordValues } from './backRecordOnSiteOutputModel'
import { storedEstimatedWeight, storedMeasuredWeight } from './backRecordSourceRolls'
export type { OnSiteOutputRecordValues } from './backRecordOnSiteOutputModel'

export interface RollRecordValues {
  actualGramWeight?: number
  actualWidth?: number
  actualWeight?: number
  weightEntryMode?: WeightEntryMode
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
  warehouseUuid?: string
  rolls?: Record<string, RollRecordValues>
  finishes?: Record<string, FinishRecordValues>
  finishAdjustments?: Record<string, BackRecordFinishAdjustmentValues>
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

export interface BackRecordBuildOptions {
  completeOrder?: boolean
  selectedFinishUuids?: Set<string>
  selectedItemKeys?: Set<string>
  selectedRollUuids?: Set<string>
}

export function isActiveBackRecordFinish(finish: FinishRoll): boolean {
  return finish.rollNoStatus !== 3 && finish.sourceType !== 2 && finish.finishStatus !== 4
}

export function activeFinishRolls(detail?: ProcessOrderDetailVO | null): FinishRoll[] {
  return (detail?.finishRolls ?? []).filter(isActiveBackRecordFinish)
}

export function initialBackRecordValues(detail: ProcessOrderDetailVO): BackRecordFormValues {
  return {
    rolls: Object.fromEntries(detail.originalRolls.map((roll) => [roll.uuid, rollValues(roll)])),
    finishes: Object.fromEntries(activeFinishRolls(detail).map((finish) => [finish.uuid, finishValues(finish)])),
    finishAdjustments: {},
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
  options?: BackRecordBuildOptions,
): BackRecordDTO {
  const selectedRolls = options?.selectedRollUuids
    ?? new Set(detail.originalRolls.filter((roll) => roll.isChecked !== 1).map((roll) => roll.uuid))
  const selectedOutputs = filterOnSiteOutputs(values.onSiteOutputs, options?.selectedItemKeys)
  const onSite = buildOnSiteOutputSubmission(detail, selectedOutputs)
  const finishSources = finishSourceMap(detail)
  const finishes = activeFinishRolls(detail)
    .filter((finish) => finishInSelection(
      finish,
      values,
      selectedRolls,
      finishSources,
      options?.selectedFinishUuids,
    ))
    .filter((finish) => !onSite.configuredUuids.has(finish.uuid) && !onSite.managedUuids.has(finish.uuid))
    .map((finish) => toFinishDTO(
      finish,
      values.finishes?.[finish.uuid],
      finishAction(finish.uuid, values, options?.selectedItemKeys),
      finishReason(finish.uuid, values, options?.selectedItemKeys),
    ))
  finishes.push(...onSite.finishes)
  finishes.push(...addedFinishDTOs(values, options?.selectedItemKeys, options?.selectedRollUuids))
  const trims = [...toLegacyTrimDTOs(values.trims), ...onSite.trims]
    .filter((trim) => selectedRolls.has(trim.originalUuid))
  return {
    expectedVersion: detail.order.version ?? 0,
    completeOrder: options?.completeOrder ?? true,
    warehouseUuid: values.warehouseUuid ?? '',
    releaseAdminUsername: authorization?.releaseAdminUsername,
    releaseAdminPassword: authorization?.releaseAdminPassword,
    releaseReason: authorization?.releaseReason,
    varianceReason: variance?.varianceReason,
    rolls: detail.originalRolls
      .filter((roll) => selectedRolls.has(roll.uuid))
      .map((roll) => toRollDTO(roll, values.rolls?.[roll.uuid])),
    finishes: finishes.length > 0 ? finishes : undefined,
    trims: trims.length > 0 ? trims : undefined,
    steps: stepsInBackRecordSelection(detail, selectedRolls)
      .map((step) => toStepDTO(step, values.steps?.[step.uuid])),
  }
}

export function stepsInBackRecordSelection(
  detail: ProcessOrderDetailVO,
  selectedRolls: Set<string>,
): ProcessStep[] {
  if (selectedRolls.size === 0) return detail.steps
  const productionStepUuids = new Set(
    (detail.rollProductions ?? [])
      .filter((production) => production.originalUuid && selectedRolls.has(production.originalUuid))
      .flatMap((production) => production.steps ?? [])
      .map((step) => step.uuid),
  )
  return detail.steps.filter((step) => (
    Boolean(step.originalUuid && selectedRolls.has(step.originalUuid))
    || productionStepUuids.has(step.uuid)
  ))
}

function filterOnSiteOutputs(
  outputs: BackRecordFormValues['onSiteOutputs'],
  selectedItemKeys?: Set<string>,
) {
  if (!outputs || !selectedItemKeys) return outputs
  return Object.fromEntries(
    Object.entries(outputs).filter(([key]) => selectedItemKeys.has(key)),
  )
}

function finishSourceMap(detail: ProcessOrderDetailVO) {
  const result = new Map<string, Set<string>>()
  for (const production of detail.rollProductions ?? []) {
    for (const finish of production.finishes ?? []) {
      const sources = result.get(finish.uuid) ?? new Set<string>()
      for (const source of finish.sources ?? []) {
        if (source.originalUuid) sources.add(source.originalUuid)
      }
      if (production.originalUuid) sources.add(production.originalUuid)
      result.set(finish.uuid, sources)
    }
  }
  return result
}

function finishInSelection(
  finish: FinishRoll,
  values: BackRecordFormValues,
  selectedRolls: Set<string>,
  sourceMap: Map<string, Set<string>>,
  selectedFinishUuids?: Set<string>,
) {
  const sources = sourceMap.get(finish.uuid)
  if (sources?.size) return Array.from(sources).every((uuid) => selectedRolls.has(uuid))
  const selectedSource = values.finishes?.[finish.uuid]?.originalUuid
  if (selectedSource) return selectedRolls.has(selectedSource)
  if (selectedFinishUuids) return selectedFinishUuids.has(finish.uuid)
  return selectedRolls.size > 0
}

export function fillRollActuals(detail: ProcessOrderDetailVO): BackRecordFormValues['rolls'] {
  return Object.fromEntries(detail.originalRolls.map((roll) => [roll.uuid, {
    actualGramWeight: roll.actualGramWeight ?? roll.gramWeight,
    actualWidth: roll.actualWidth ?? roll.originalWidth,
    actualWeight: storedMeasuredWeight(roll) ?? nominalWeight(roll),
    weightEntryMode: storedMeasuredWeight(roll) != null ? 'MEASURED' : nominalWeight(roll) != null ? 'CARRY_NOMINAL' : undefined,
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
    ?? checks.find((check) => check.level === 'UNVERIFIED')
    ?? checks[0]
}

function rollValues(roll: OriginalRoll): RollRecordValues {
  const measured = storedMeasuredWeight(roll)
  const estimated = storedEstimatedWeight(roll)
  const nominal = nominalWeight(roll)
  return {
    actualGramWeight: roll.actualGramWeight,
    actualWidth: roll.actualWidth,
    actualWeight: measured ?? estimated ?? nominal,
    weightEntryMode: measured != null ? 'MEASURED'
      : estimated != null ? 'USER_ESTIMATE'
        : nominal != null ? 'CARRY_NOMINAL' : undefined,
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
    lossWeight: step.lossWeight ?? step.plannedLossWeight,
    knifeCount: step.knifeCount,
  }
}

function toRollDTO(roll: OriginalRoll, values?: RollRecordValues): BackRecordRollDTO {
  return {
    uuid: roll.uuid,
    actualGramWeight: values?.actualGramWeight,
    actualWidth: values?.actualWidth,
    actualWeight: values?.actualWeight,
    weightEntryMode: values?.weightEntryMode,
    remark: values?.remark,
  }
}

function nominalWeight(roll: OriginalRoll): number | undefined {
  if (roll.weightStatus === 'UNKNOWN') return undefined
  if (roll.totalWeight != null && roll.totalWeight > 0) return roll.totalWeight
  if (roll.rollWeight == null || roll.rollWeight <= 0) return undefined
  return roll.rollWeight * (roll.pieceNum ?? 1)
}

function toFinishDTO(
  finish: FinishRoll,
  values?: FinishRecordValues,
  productionAction?: BackRecordFinishAction,
  productionAdjustmentReason?: string,
): BackRecordFinishDTO {
  return {
    uuid: finish.uuid,
    originalUuid: values?.originalUuid,
    productionAction,
    productionAdjustmentReason,
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

function finishAction(
  finishUuid: string,
  values: BackRecordFormValues,
  selectedItemKeys?: Set<string>,
): BackRecordFinishAction | undefined {
  const adjustment = findAdjustmentForFinish(finishUuid, values.finishAdjustments, selectedItemKeys)
  if (!adjustment) return undefined
  return adjustment.producedFinishUuids.includes(finishUuid) ? 'PRODUCED' : 'NOT_PRODUCED'
}

function finishReason(
  finishUuid: string,
  values: BackRecordFormValues,
  selectedItemKeys?: Set<string>,
): string | undefined {
  return findAdjustmentForFinish(finishUuid, values.finishAdjustments, selectedItemKeys)?.reason
}

function findAdjustmentForFinish(
  finishUuid: string,
  adjustments?: Record<string, BackRecordFinishAdjustmentValues>,
  selectedItemKeys?: Set<string>,
) {
  return Object.entries(adjustments ?? {})
    .filter(([key]) => !selectedItemKeys || selectedItemKeys.has(key))
    .map(([, adjustment]) => adjustment)
    .find((adjustment) => adjustment.plannedFinishUuids.includes(finishUuid))
}

function addedFinishDTOs(
  values: BackRecordFormValues,
  selectedItemKeys?: Set<string>,
  selectedRollUuids?: Set<string>,
): BackRecordFinishDTO[] {
  return Object.entries(values.finishAdjustments ?? {})
    .filter(([key]) => !selectedItemKeys || selectedItemKeys.has(key))
    .flatMap(([, adjustment]) => adjustment.added)
    .filter((added) => !selectedRollUuids || selectedRollUuids.has(added.originalUuid))
    .map((added) => ({
      ...valuesToFinishDTO(values.finishes?.[added.uuid]),
      uuid: undefined,
      originalUuid: values.finishes?.[added.uuid]?.originalUuid ?? added.originalUuid,
      productionAction: 'ADDED' as const,
      productionAdjustmentReason: findAdjustmentReason(values.finishAdjustments, added.uuid),
    }))
}

function valuesToFinishDTO(values?: FinishRecordValues): Omit<BackRecordFinishDTO, 'uuid' | 'originalUuid'> {
  return {
    finishWidth: values?.finishWidth,
    finishDiameter: values?.finishDiameter,
    finishCoreDiameter: values?.finishCoreDiameter,
    actualWeight: values?.actualWeight,
    scrapWeight: values?.scrapWeight,
    isRemain: values?.isRemain ?? 0,
    isAbnormal: values?.isAbnormal,
    abnormalType: values?.abnormalType,
    actualRemark: values?.actualRemark,
  }
}

function findAdjustmentReason(
  adjustments?: Record<string, BackRecordFinishAdjustmentValues>,
  addedUuid?: string,
): string | undefined {
  return Object.values(adjustments ?? {}).find((adjustment) => adjustment.added.some((added) => added.uuid === addedUuid))?.reason
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

function positive(value?: number) {
  return value != null && value > 0
}

function validWidth(value?: number) {
  return positive(value) ? value : undefined
}
