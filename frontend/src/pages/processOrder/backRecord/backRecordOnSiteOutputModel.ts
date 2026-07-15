import type {
  BackRecordFinishDTO,
  BackRecordTrimDTO,
  FinishRoll,
  OriginalRoll,
  ProcessOrderDetailVO,
  RollProductionVO,
} from '../../../types/processOrder'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'

export interface OnSiteOutputRecordValues {
  uuid?: string
  finishRollNo?: string
  outputType: 'FINISH' | 'TRIM'
  originalUuid?: string
  finishWidth?: number
  finishDiameter?: number
  finishCoreDiameter?: number
  actualWeight?: number
  scrapWeight?: number
  isAbnormal?: number
  abnormalType?: string
  actualRemark?: string
}

export function buildOnSiteOutputSubmission(
  detail: ProcessOrderDetailVO,
  groups?: Record<string, Array<OnSiteOutputRecordValues | undefined>>,
) {
  const outputs = Object.values(groups ?? {})
    .flat()
    .filter((output): output is OnSiteOutputRecordValues => output != null)
  return {
    configuredUuids: configuredOnSiteFinishUuids(detail),
    managedUuids: new Set(outputs.flatMap((output) => output.uuid ? [output.uuid] : [])),
    finishes: outputs.filter((output) => output.outputType === 'FINISH').map(toFinishDTO),
    trims: outputs.filter((output) => output.outputType === 'TRIM').map(toTrimDTO),
  }
}

export function toLegacyTrimDTOs(groups?: Record<string, Array<{
  originalUuid?: string
  finishWidth?: number
  actualWeight?: number
  actualRemark?: string
}>>): BackRecordTrimDTO[] {
  return Object.values(groups ?? {}).flatMap((rows) => rows.map((row) => ({
    originalUuid: row.originalUuid ?? '',
    finishWidth: row.finishWidth ?? 0,
    actualWeight: row.actualWeight ?? 0,
    ...(row.actualRemark ? { actualRemark: row.actualRemark } : {}),
  })))
}

export function existingFinishSourceUuid(
  productions: RollProductionVO[],
  finishUuid: string,
): string | undefined {
  const sources = productions
    .flatMap((production) => production.finishes ?? [])
    .find((finish) => finish.uuid === finishUuid)?.sources
    ?.flatMap((source) => source.originalUuid ? [source.originalUuid] : []) ?? []
  return new Set(sources).size === 1 ? sources[0] : undefined
}

export function buildInitialOnSiteOutputGroups(
  detail: ProcessOrderDetailVO,
  items: BackRecordWorkItem[],
): Record<string, OnSiteOutputRecordValues[]> {
  return Object.fromEntries(items
    .filter((item) => item.roll?.processMode === 2)
    .map((item) => [item.key, initialOnSiteOutputRows(item, detail.originalRolls)]))
}

function initialOnSiteOutputRows(
  item: BackRecordWorkItem,
  rolls: OriginalRoll[],
): OnSiteOutputRecordValues[] {
  const sourceUuids = itemSourceUuids(item)
  const eligibleSources = rolls.filter((roll) => roll.processMode === 2 && sourceUuids.has(roll.uuid))
  return item.finishes
    .filter(({ finish }) => finish.isSpare !== 1 && finish.isRemain !== 1)
    .map(({ finish }) => outputFromFinish(finish, item.rollProductions, eligibleSources))
}

function outputFromFinish(
  finish: FinishRoll,
  productions: RollProductionVO[],
  sources: OriginalRoll[],
): OnSiteOutputRecordValues {
  return {
    uuid: finish.uuid,
    finishRollNo: finish.finishRollNo,
    outputType: 'FINISH',
    originalUuid: existingFinishSourceUuid(productions, finish.uuid)
      ?? (sources.length === 1 ? sources[0]?.uuid : undefined),
    finishWidth: finish.finishWidth && finish.finishWidth > 0 ? finish.finishWidth : undefined,
    finishDiameter: finish.finishDiameter,
    finishCoreDiameter: finish.finishCoreDiameter,
    actualWeight: finish.actualWeight,
    isAbnormal: finish.isAbnormal,
    abnormalType: finish.abnormalType,
    actualRemark: finish.actualRemark,
  }
}

function itemSourceUuids(item: BackRecordWorkItem): Set<string> {
  const sourceUuids = new Set(item.rollProductions.flatMap((production) =>
    production.originalUuid ? [production.originalUuid] : []))
  if (item.roll) sourceUuids.add(item.roll.uuid)
  return sourceUuids
}

function configuredOnSiteFinishUuids(detail: ProcessOrderDetailVO): Set<string> {
  const rollUuids = new Set(detail.originalRolls.filter((roll) => roll.processMode === 2).map((roll) => roll.uuid))
  return new Set((detail.rollProductions ?? [])
    .filter((production) => production.originalUuid != null && rollUuids.has(production.originalUuid))
    .flatMap((production) => (production.finishes ?? []).map((finish) => finish.uuid)))
}

function toFinishDTO(output: OnSiteOutputRecordValues): BackRecordFinishDTO {
  return {
    uuid: output.uuid,
    originalUuid: output.originalUuid,
    finishWidth: output.finishWidth,
    finishDiameter: output.finishDiameter,
    finishCoreDiameter: output.finishCoreDiameter,
    actualWeight: output.actualWeight,
    scrapWeight: output.scrapWeight,
    isRemain: 0,
    isAbnormal: output.isAbnormal ?? 0,
    abnormalType: output.abnormalType,
    actualRemark: output.actualRemark,
  }
}

function toTrimDTO(output: OnSiteOutputRecordValues): BackRecordTrimDTO {
  return {
    originalUuid: output.originalUuid ?? '',
    finishWidth: output.finishWidth ?? 0,
    actualWeight: output.actualWeight ?? 0,
    actualRemark: output.actualRemark,
  }
}
