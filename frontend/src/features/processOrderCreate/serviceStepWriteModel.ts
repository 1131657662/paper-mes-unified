import type { ProcessStep } from '../../types/processOrder'
import type { ProcessStepDTO } from '../../api/processOrder'

export type ServiceStepWriteTarget =
  | { kind: 'create' }
  | { kind: 'duplicate' }
  | { kind: 'update'; stepUuid: string }

export function resolveServiceStepWriteTarget(
  steps: ProcessStep[],
  stepType: number,
  requestedStepUuid?: string,
): ServiceStepWriteTarget {
  const matching = steps.filter((step) => step.stepType === stepType)
  if (matching.length > 1) return { kind: 'duplicate' }
  const stepUuid = requestedStepUuid ?? matching[0]?.uuid
  return stepUuid ? { kind: 'update', stepUuid } : { kind: 'create' }
}

export function serviceStepIsAbsent(steps: ProcessStep[] | undefined, stepUuid: string): boolean {
  return steps !== undefined && !steps.some((step) => step.uuid === stepUuid)
}

export function serviceStepMatchesRequest(step: ProcessStep, request: ProcessStepDTO): boolean {
  const billingMode = request.billingMode ?? 1
  return step.originalUuid === request.originalUuid
    && step.stepType === request.stepType
    && step.isMain !== 1
    && optionalMatches(step.stepName, request.stepName)
    && optionalMatches(step.machineUuid, request.machineUuid)
    && step.billingMode === billingMode
    && pricingMatches(step, request, billingMode)
    && optionalMatches(step.remark, request.remark)
}

export function serviceStepsMatchRequests(
  steps: ProcessStep[] | undefined,
  requests: ProcessStepDTO[],
): boolean {
  if (!steps) return false
  return requests.every((request) => steps.some((step) => serviceStepMatchesRequest(step, request)))
}

function pricingMatches(step: ProcessStep, request: ProcessStepDTO, billingMode: number): boolean {
  if (billingMode === 3) return numericMatches(step.billingAmount, request.billingAmount)
  if (billingMode === 4) return true
  return optionalMatches(step.billingBasis, request.billingBasis?.trim().toUpperCase())
    && numericMatches(step.unitPrice, request.unitPrice)
}

function optionalMatches(actual: unknown, expected: unknown): boolean {
  if (expected === undefined || expected === null || expected === '') return true
  return actual === expected
}

function numericMatches(actual: number | undefined, expected: number | undefined): boolean {
  return expected === undefined || Number(actual) === Number(expected)
}
