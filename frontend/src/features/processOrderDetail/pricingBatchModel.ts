import type {
  ProcessStepPricingBatchDTO,
  ProcessStepPricingBatchGroupDTO,
} from '../../api/processOrder'
import type { ProcessStep } from '../../types/processOrder'

export interface PricingBatchFormValues {
  reason: string
  sawPrice?: number
  sawRestore?: boolean
  rewindPrice?: number
  rewindRestore?: boolean
}

export type PricingStep = ProcessStep & { stepType: 1 | 2 }

export function pricingSteps(steps: ProcessStep[] = []): PricingStep[] {
  return steps.filter((step): step is PricingStep => step.stepType === 1 || step.stepType === 2)
}

export function buildPricingBatchRequest(options: {
  orderVersion: number
  selectedSteps: PricingStep[]
  values: PricingBatchFormValues
  requestId?: string
}): ProcessStepPricingBatchDTO {
  const { orderVersion, selectedSteps, values, requestId } = options
  return {
    expectedOrderVersion: orderVersion,
    reason: values.reason.trim(),
    requestId,
    groups: ([1, 2] as const).flatMap((stepType) => buildGroup(stepType, selectedSteps, values)),
  }
}

export function initialPricingValues(steps: PricingStep[]): Partial<PricingBatchFormValues> {
  return {
    reason: '',
    sawPrice: commonPrice(steps.filter((step) => step.stepType === 1)),
    sawRestore: false,
    rewindPrice: commonPrice(steps.filter((step) => step.stepType === 2)),
    rewindRestore: false,
  }
}

function buildGroup(stepType: 1 | 2, steps: PricingStep[], values: PricingBatchFormValues) {
  const selected = steps.filter((step) => step.stepType === stepType)
  if (!selected.length) return []
  const restoreStandard = stepType === 1 ? Boolean(values.sawRestore) : Boolean(values.rewindRestore)
  const billingUnitPrice = stepType === 1 ? values.sawPrice : values.rewindPrice
  const group: ProcessStepPricingBatchGroupDTO = {
    stepType,
    stepUuids: selected.map((step) => step.uuid),
    restoreStandard,
    billingUnitPrice: restoreStandard ? undefined : billingUnitPrice,
  }
  return [group]
}

function commonPrice(steps: PricingStep[]): number | undefined {
  if (!steps.length) return undefined
  const prices = new Set(steps.map((step) => step.billingUnitPrice ?? step.unitPrice))
  if (prices.size !== 1) return undefined
  return prices.values().next().value
}
