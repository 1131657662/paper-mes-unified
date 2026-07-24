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
  stripMode?: 1 | 3 | 4
  stripBasis?: 'PIECE' | 'TON'
  stripPrice?: number
  stripAmount?: number
  repackMode?: 1 | 3 | 4
  repackBasis?: 'PIECE' | 'TON'
  repackPrice?: number
  repackAmount?: number
}

export type PricingStep = ProcessStep & { stepType: 1 | 2 | 3 | 4 }

export function pricingSteps(steps: ProcessStep[] = []): PricingStep[] {
  return steps.filter((step): step is PricingStep => [1, 2, 3, 4].includes(step.stepType ?? 0))
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
    groups: ([1, 2, 3, 4] as const).flatMap((stepType) => buildGroup(stepType, selectedSteps, values)),
  }
}

export function initialPricingValues(steps: PricingStep[]): Partial<PricingBatchFormValues> {
  return {
    reason: '',
    sawPrice: commonPrice(steps.filter((step) => step.stepType === 1)),
    sawRestore: false,
    rewindPrice: commonPrice(steps.filter((step) => step.stepType === 2)),
    rewindRestore: false,
    ...initialServiceValues('strip', steps.filter((step) => step.stepType === 3)),
    ...initialServiceValues('repack', steps.filter((step) => step.stepType === 4)),
  }
}

function buildGroup(stepType: 1 | 2 | 3 | 4, steps: PricingStep[], values: PricingBatchFormValues) {
  const selected = steps.filter((step) => step.stepType === stepType)
  if (!selected.length) return []
  if (stepType === 3 || stepType === 4) return [buildServiceGroup(stepType, selected, values)]
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

function buildServiceGroup(stepType: 3 | 4, steps: PricingStep[], values: PricingBatchFormValues) {
  const prefix = stepType === 3 ? 'strip' : 'repack'
  const mode = values[`${prefix}Mode`] ?? 1
  const group: ProcessStepPricingBatchGroupDTO = {
    stepType,
    stepUuids: steps.map((step) => step.uuid),
    restoreStandard: false,
    billingMode: mode,
  }
  if (mode === 1) {
    group.billingBasis = values[`${prefix}Basis`]
    group.billingUnitPrice = values[`${prefix}Price`]
  }
  if (mode === 3) group.billingAmount = values[`${prefix}Amount`]
  return group
}

function initialServiceValues(prefix: 'strip' | 'repack', steps: PricingStep[]) {
  if (!steps.length) return {}
  const modes = new Set(steps.map((step) => step.billingMode ?? 1))
  const mode = (modes.size === 1 ? modes.values().next().value : 1) as 1 | 3 | 4
  const bases = new Set(steps.map((step) => step.billingBasis).filter(Boolean))
  const basis = (bases.size === 1 ? bases.values().next().value : 'PIECE') as 'PIECE' | 'TON'
  return {
    [`${prefix}Mode`]: mode,
    [`${prefix}Basis`]: basis,
    [`${prefix}Price`]: commonPrice(steps),
    [`${prefix}Amount`]: mode === 3
      ? steps.reduce((sum, step) => sum + (step.billingAmount ?? 0), 0)
      : undefined,
  }
}

function commonPrice(steps: PricingStep[]): number | undefined {
  if (!steps.length) return undefined
  const prices = new Set(steps.map((step) => step.billingUnitPrice ?? step.unitPrice))
  if (prices.size !== 1) return undefined
  return prices.values().next().value
}
