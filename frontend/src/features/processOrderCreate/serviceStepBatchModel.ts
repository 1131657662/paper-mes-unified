import type { ProcessStepDTO } from '../../api/processOrder'
import type { ProcessStep } from '../../types/processOrder'
import type { RollDraft } from './types'

export type FixedAmountScope = 'TOTAL' | 'EACH'

export function buildServiceStepBatch(
  template: ProcessStepDTO,
  originalUuids: string[],
  fixedAmountScope: FixedAmountScope = 'TOTAL',
): ProcessStepDTO[] {
  if (template.billingMode !== 3 || template.billingAmount == null || fixedAmountScope === 'EACH') {
    return originalUuids.map((originalUuid) => ({ ...template, originalUuid }))
  }
  const amounts = distributeFixedTotal(template.billingAmount, originalUuids.length)
  return originalUuids.map((originalUuid, index) => ({
    ...template,
    originalUuid,
    billingAmount: amounts[index],
  }))
}

export function distributeFixedTotal(total: number, count: number): number[] {
  if (count <= 0) return []
  const totalCents = Math.round((total + Number.EPSILON) * 100)
  const baseCents = Math.floor(totalCents / count)
  const remainderCents = totalCents - baseCents * count
  return Array.from(
    { length: count },
    (_, index) => (baseCents + (index < remainderCents ? 1 : 0)) / 100,
  )
}

export function serviceStepsForRoll(steps: ProcessStep[] = [], originalUuid?: string): ProcessStep[] {
  return steps.filter((step) => step.originalUuid === originalUuid && step.isMain !== 1
    && (step.stepType === 3 || step.stepType === 4))
}

export function serviceStepTemplate(step: ProcessStep): ProcessStepDTO | undefined {
  if (!step.originalUuid || step.stepType == null) return undefined
  return {
    originalUuid: step.originalUuid,
    stepType: step.stepType,
    stepName: step.stepName,
    machineUuid: step.machineUuid,
    isMain: step.isMain,
    billingBasis: step.billingBasis,
    serviceQuantity: step.serviceQuantity,
    billingMode: step.billingMode,
    billingAmount: step.billingAmount,
    unitPrice: step.unitPrice ?? step.billingUnitPrice,
    remark: step.remark,
  }
}

export function resolveServiceApplyTargets(options: ServiceApplyTargetOptions): ServiceApplyTargets {
  const existingRolls = new Set((options.steps ?? [])
    .filter((step) => step.isMain !== 1 && step.stepType === options.stepType)
    .map((step) => step.originalUuid))
  const targetUuids = options.rolls
    .filter((roll) => roll.processMode === 1 || roll.processMode === 2 || roll.processMode === 4)
    .map((roll) => roll.uuid)
    .filter((uuid): uuid is string => Boolean(uuid))
  const updateCount = targetUuids.filter((uuid) => existingRolls.has(uuid)).length
  return {
    targetUuids,
    createCount: targetUuids.length - updateCount,
    updateCount,
    excludedCount: options.rolls.length - targetUuids.length,
  }
}

export function countServiceApplyTargets(options: ServiceApplyTargetOptions): number {
  if (options.stepType == null) return 0
  return resolveServiceApplyTargets(options).targetUuids.length
}

interface ServiceApplyTargetOptions {
  rolls: RollDraft[]
  stepType?: number
  steps?: ProcessStep[]
}

export interface ServiceApplyTargets {
  targetUuids: string[]
  createCount: number
  updateCount: number
  excludedCount: number
}
