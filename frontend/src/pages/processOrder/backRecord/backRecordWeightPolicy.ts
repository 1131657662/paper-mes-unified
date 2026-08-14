import type { ProcessStep } from '../../../types/processOrder'
import { workItemSourceRolls } from './backRecordSourceRolls'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'

const STANDARD_BILLING_MODE = 1
const REWIND_STEP_TYPE = 2

export function requiresMeasuredSourceWeights(item: BackRecordWorkItem): boolean {
  return workItemSteps(item).some(isStandardTonnageRewind)
}

export function requiredWeightRollUuids(items: BackRecordWorkItem[]): Set<string> {
  return new Set(items
    .filter(requiresMeasuredSourceWeights)
    .flatMap((item) => workItemSourceRolls(item).map((source) => source.uuid)))
}

export function isStandardTonnageRewind(step: ProcessStep): boolean {
  return step.stepType === REWIND_STEP_TYPE
    && (step.billingMode ?? STANDARD_BILLING_MODE) === STANDARD_BILLING_MODE
}

function workItemSteps(item: BackRecordWorkItem): ProcessStep[] {
  const productions = item.rollProductions.length
    ? item.rollProductions
    : item.production ? [item.production] : []
  return Array.from(new Map(
    productions.flatMap((production) => production.steps ?? [])
      .map((step) => [step.uuid, step]),
  ).values())
}
